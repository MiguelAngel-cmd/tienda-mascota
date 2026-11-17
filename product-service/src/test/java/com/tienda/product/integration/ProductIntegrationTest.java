package com.tienda.product.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prueba de integración: levanta la aplicación completa (controlador + servicio + JPA) sobre H2
 * y la consume por HTTP con MockMvc. Corresponde al Caso 1 del enunciado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ProductIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private String producto(String codigo, String precio) {
        return """
                {"nombre":"Concentrado Cachorro","categoria":"ALIMENTOS","marca":"ProPlan",
                 "descripcion":"Bulto 3 kg","precio":%s,"codigoBarras":"%s"}
                """.formatted(precio, codigo);
    }

    private long crear(String codigo) throws Exception {
        String body = mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content(producto(codigo, "45000")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(body);
        return json.get("id").asLong();
    }

    @Test
    @DisplayName("Caso 1: registrar un producto responde 201 CREATED con Location")
    void caso1_registrarProducto_responde201() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(producto("IT-001", "45000")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/products/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void codigoDeBarrasDuplicado_responde409() throws Exception {
        crear("IT-002");
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(producto("IT-002", "1000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("IT-002")));
    }

    @Test
    void precioNegativo_responde400ConDetalle() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON).content(producto("IT-003", "-5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", containsString("precio")));
    }

    @Test
    void categoriaInvalida_responde400() throws Exception {
        mvc.perform(post("/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\",\"categoria\":\"MUEBLES\",\"precio\":1,\"codigoBarras\":\"IT-004\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crudCompleto_consultarActualizarYEliminar() throws Exception {
        long id = crear("IT-005");

        mvc.perform(get("/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoBarras").value("IT-005"));
        mvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigoBarras", hasItem("IT-005")));

        mvc.perform(put("/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(producto("IT-005", "50000").replace("Cachorro", "Adulto")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Concentrado Adulto"))
                .andExpect(jsonPath("$.precio").value(50000));

        mvc.perform(delete("/products/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/products/{id}", id)).andExpect(status().isNotFound());
    }
}
