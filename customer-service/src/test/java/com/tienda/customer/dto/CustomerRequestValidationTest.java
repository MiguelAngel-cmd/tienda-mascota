package com.tienda.customer.dto;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** Verifica las reglas de validación declaradas con anotaciones en el DTO. */
class CustomerRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void clienteValido_noTieneErrores() {
        var req = new CustomerRequest("Ana", "Pérez", "123", "3001234567", "ana@mail.com", "Calle 1");
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void nombreVacio_esInvalido() {
        var req = new CustomerRequest("  ", null, null, "3001234567", null, null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void telefonoVacio_esInvalido() {
        var req = new CustomerRequest("Ana", null, null, "", null, null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void correoSinArroba_esInvalido() {
        var req = new CustomerRequest("Ana", null, null, "3001234567", "ana.mail.com", null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void correoSinDominio_esInvalido() {
        var req = new CustomerRequest("Ana", null, null, "3001234567", "ana@mail", null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void sinCorreo_esValido() {
        var req = new CustomerRequest("Ana", null, null, "3001234567", null, null);
        assertTrue(validator.validate(req).isEmpty());
    }
}
