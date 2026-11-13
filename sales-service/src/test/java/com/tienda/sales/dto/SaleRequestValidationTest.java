package com.tienda.sales.dto;

import static org.junit.jupiter.api.Assertions.*;

import com.tienda.sales.model.EstadoVenta;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica las reglas de validación declaradas con anotaciones en los DTO de venta. */
class SaleRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void ventaValida_noTieneErrores() {
        var req = new SaleRequest(1L, List.of(new SaleItemRequest(10L, 2)));
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void ventaSinCliente_esInvalida() {
        var req = new SaleRequest(null, List.of(new SaleItemRequest(10L, 2)));
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void ventaSinProductos_esInvalida() {
        var req = new SaleRequest(1L, List.of());
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void productoConCantidadCero_esInvalido() {
        var req = new SaleRequest(1L, List.of(new SaleItemRequest(10L, 0)));
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void productoSinId_esInvalido() {
        var req = new SaleRequest(1L, List.of(new SaleItemRequest(null, 1)));
        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void actualizacionSinEstado_esInvalida() {
        assertFalse(validator.validate(new SaleUpdateRequest(null)).isEmpty());
        assertTrue(validator.validate(new SaleUpdateRequest(EstadoVenta.CANCELADA)).isEmpty());
    }
}
