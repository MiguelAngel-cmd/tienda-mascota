package com.tienda.product.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.tienda.product.model.Categoria;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Verifica las reglas de validación declaradas con anotaciones en el DTO. */
class ProductRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void productoValido_noTieneErrores() {
        var req = new ProductRequest("Collar", Categoria.ACCESORIOS, "Pet", "Ajustable",
                new BigDecimal("15000"), "777", null);
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void nombreVacio_esInvalido() {
        var req = new ProductRequest("  ", Categoria.ACCESORIOS, null, null, BigDecimal.TEN, "777", null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void precioNegativo_esInvalido() {
        var req = new ProductRequest("Collar", Categoria.ACCESORIOS, null, null, new BigDecimal("-1"), "777", null);
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void sinCategoria_esInvalido() {
        var req = new ProductRequest("Collar", null, null, null, BigDecimal.TEN, "777", null);
        assertFalse(validator.validate(req).isEmpty());
    }
}
