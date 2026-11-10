package com.tienda.inventory.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.tienda.inventory.model.TipoMovimiento;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

/** Verifica las reglas de validación declaradas con anotaciones en los DTO de inventario. */
class InventoryRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void inventarioValido_noTieneErrores() {
        assertTrue(validator.validate(new InventoryRequest(1L, 10, 3)).isEmpty());
    }

    @Test
    void stockNegativo_esInvalido() {
        assertFalse(validator.validate(new InventoryRequest(1L, -1, 3)).isEmpty());
    }

    @Test
    void stockMinimoNegativo_esInvalido() {
        assertFalse(validator.validate(new InventoryRequest(1L, 10, -2)).isEmpty());
    }

    @Test
    void sinProductId_esInvalido() {
        assertFalse(validator.validate(new InventoryRequest(null, 10, 3)).isEmpty());
    }

    @Test
    void movimientoConCantidadNegativa_esInvalido() {
        assertFalse(validator.validate(new MovementRequest(TipoMovimiento.ENTRADA, -5)).isEmpty());
    }

    @Test
    void movimientoSinTipo_esInvalido() {
        assertFalse(validator.validate(new MovementRequest(null, 5)).isEmpty());
    }
}
