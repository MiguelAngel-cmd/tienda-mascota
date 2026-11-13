package com.tienda.sales.dto;

import com.tienda.sales.model.EstadoVenta;
import jakarta.validation.constraints.NotNull;

/** Cambio de estado de una venta (PUT). Por ahora solo se permite CANCELADA. */
public record SaleUpdateRequest(
        @NotNull(message = "El estado es obligatorio") EstadoVenta estado) {
}
