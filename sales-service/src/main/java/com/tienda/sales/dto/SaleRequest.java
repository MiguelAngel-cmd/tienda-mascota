package com.tienda.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Datos para registrar una venta. El precio no se envía: se consulta en Product-Service. */
public record SaleRequest(
        @NotNull(message = "El customerId es obligatorio") Long customerId,
        @NotEmpty(message = "La venta debe tener al menos un producto") @Valid List<SaleItemRequest> items) {
}
