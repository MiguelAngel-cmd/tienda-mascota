package com.tienda.inventory.integration;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tienda.inventory.client.ProductClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integración del Inventory-Service (Caso 2). El Product-Service se simula con un
 * MockBean; la prueba de punta a punta con los servicios reales está en la colección Postman.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class InventoryIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    @MockBean
    ProductClient productClient;

    private long registrar(long productId, int stock, int minimo) throws Exception {
        when(productClient.existe(productId)).thenReturn(true);
        String body = mvc.perform(post("/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"stockActual\":%d,\"stockMinimo\":%d}"
                                .formatted(productId, stock, minimo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private String movimiento(String tipo, int cantidad) {
        return "{\"tipoMovimiento\":\"%s\",\"cantidad\":%d}".formatted(tipo, cantidad);
    }

    @Test
    @DisplayName("Caso 2: agregar existencias valida el producto y actualiza el inventario")
    void caso2_agregarExistencias() throws Exception {
        registrar(1L, 10, 3);

        mvc.perform(post("/inventory/1/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento("ENTRADA", 15)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockActual").value(25));

        mvc.perform(get("/inventory/1/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].tipoMovimiento", containsInAnyOrder("ENTRADA", "ENTRADA")));
    }

    @Test
    void inventarioDeProductoInexistente_responde400() throws Exception {
        when(productClient.existe(999L)).thenReturn(false);
        mvc.perform(post("/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":999,\"stockActual\":5,\"stockMinimo\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("no existe")));
    }

    @Test
    void inventarioDuplicado_responde409() throws Exception {
        registrar(2L, 1, 1);
        mvc.perform(post("/inventory").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":2,\"stockActual\":5,\"stockMinimo\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void salidaMayorAlStock_responde400YNoCambiaElStock() throws Exception {
        registrar(3L, 4, 1);
        mvc.perform(post("/inventory/3/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento("SALIDA", 5)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/inventory/3")).andExpect(jsonPath("$.stockActual").value(4));
    }

    @Test
    void stockBajoElMinimo_apareceEnAlertas() throws Exception {
        registrar(4L, 10, 5);
        mvc.perform(post("/inventory/4/movements").contentType(MediaType.APPLICATION_JSON)
                        .content(movimiento("SALIDA", 8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertaStockBajo").value(true));
        mvc.perform(get("/inventory/alerts"))
                .andExpect(jsonPath("$[*].productId", hasItem(4)));
    }

    @Test
    void actualizarStock_registraAjuste() throws Exception {
        long id = registrar(5L, 10, 2);
        mvc.perform(put("/inventory/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockActual\":7,\"stockMinimo\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockActual").value(7));
        mvc.perform(get("/inventory/5/movements"))
                .andExpect(jsonPath("$[0].tipoMovimiento").value("AJUSTE"));
    }
}
