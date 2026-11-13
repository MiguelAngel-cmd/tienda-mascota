package com.tienda.sales.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Un producto de la venta y la cantidad que se quiere comprar. */
public record SaleItemRequest(
        @NotNull(message = "El productId es obligatorio") Long productId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1") Integer cantidad) {
}
