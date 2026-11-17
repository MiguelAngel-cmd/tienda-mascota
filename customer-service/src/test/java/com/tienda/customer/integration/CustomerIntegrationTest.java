package com.tienda.customer.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Prueba de integración del Customer-Service: HTTP + servicio + JPA sobre H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CustomerIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;

    private String cliente(String documento, String correo) {
        return """
                {"nombre":"Laura","apellido":"Gómez","documento":"%s","telefono":"3001234567",
                 "correo":"%s","direccion":"Calle 10 # 5-20"}
                """.formatted(documento, correo);
    }

    private long crear(String documento, String correo) throws Exception {
        String body = mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(cliente(documento, correo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    @Test
    void registrarCliente_responde201ConFechaDeRegistro() throws Exception {
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(cliente("1001", "laura@correo.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.fechaRegistro").isNotEmpty());
    }

    @Test
    void documentoDuplicado_responde409() throws Exception {
        crear("1002", "a1002@correo.com");
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(cliente("1002", "otro1002@correo.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void correoRepetido_sinImportarMayusculas_responde409() throws Exception {
        crear("1003", "pepe@correo.com");
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
                        .content(cliente("1004", "PEPE@correo.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void correoMalFormadoYSinTelefono_responde400() throws Exception {
        mvc.perform(post("/customers").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"telefono\":\"\",\"correo\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void crudCompleto_consultarActualizarYEliminar() throws Exception {
        long id = crear("1005", "crud@correo.com");

        mvc.perform(get("/customers/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value("1005"));
        mvc.perform(get("/customers")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].documento", hasItem("1005")));

        mvc.perform(put("/customers/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(cliente("1005", "nuevo@correo.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("nuevo@correo.com"));

        mvc.perform(delete("/customers/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/customers/{id}", id)).andExpect(status().isNotFound());
    }
}
