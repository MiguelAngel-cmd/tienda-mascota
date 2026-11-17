package com.tienda.sales.integration;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tienda.sales.client.CustomerClient;
import com.tienda.sales.client.InventoryClient;
import com.tienda.sales.client.InventoryInfo;
import com.tienda.sales.client.ProductClient;
import com.tienda.sales.client.ProductInfo;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
 * Prueba de integración del Sales-Service (Casos 3, 4 y 5). Los otros tres servicios se simulan
 * con MockBean; la versión de punta a punta con servicios reales está en la colección Postman.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SaleIntegrationTest {

    @Autowired
    MockMvc mvc;
    @MockBean
    CustomerClient customerClient;
    @MockBean
    ProductClient productClient;
    @MockBean
    InventoryClient inventoryClient;

    @BeforeEach
    void datos() {
        when(customerClient.existe(1L)).thenReturn(true);
        when(productClient.obtener(10L))
                .thenReturn(Optional.of(new ProductInfo(10L, "Arena para gato", new BigDecimal("25000"), "ACTIVO")));
        when(productClient.obtener(20L))
                .thenReturn(Optional.of(new ProductInfo(20L, "Collar", new BigDecimal("12500.50"), "ACTIVO")));
        when(inventoryClient.obtener(10L)).thenReturn(Optional.of(new InventoryInfo(10L, 50)));
        when(inventoryClient.obtener(20L)).thenReturn(Optional.of(new InventoryInfo(20L, 0)));
    }

    @Test
    @DisplayName("Caso 3: registrar una venta valida cliente y stock, y descuenta el inventario")
    void caso3_registrarVenta() throws Exception {
        mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[{\"productId\":10,\"cantidad\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/sales/")))
                .andExpect(jsonPath("$.estado").value("FACTURADA"))
                .andExpect(jsonPath("$.total").value(75000))
                .andExpect(jsonPath("$.detalles[0].subtotal").value(75000));

        verify(customerClient).existe(1L);
        verify(inventoryClient).descontar(10L, 3);
    }

    @Test
    @DisplayName("Caso 4: vender un producto sin existencias responde 400 BAD REQUEST")
    void caso4_ventaSinExistencias_responde400() throws Exception {
        mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[{\"productId\":20,\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Stock insuficiente")));

        verify(inventoryClient, never()).descontar(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Caso 5: consultar el historial de ventas")
    void caso5_historialDeVentas() throws Exception {
        mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[{\"productId\":10,\"cantidad\":1}]}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/sales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].detalles", not(empty())));
    }

    @Test
    void ventaConClienteInexistente_responde400() throws Exception {
        when(customerClient.existe(77L)).thenReturn(false);
        mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":77,\"items\":[{\"productId\":10,\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cliente")));
    }

    @Test
    void ventaSinItems_responde400() throws Exception {
        mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelarVentaFacturada_devuelveElStock() throws Exception {
        String body = mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"items\":[{\"productId\":10,\"cantidad\":2}]}"))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*?\"id\":(\\d+).*", "$1");

        mvc.perform(put("/sales/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        verify(inventoryClient).reponer(10L, 2);
    }
}
