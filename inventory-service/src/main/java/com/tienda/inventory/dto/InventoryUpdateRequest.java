package com.tienda.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Actualización directa del inventario (PUT). */
public record InventoryUpdateRequest(
        @NotNull(message = "El stock actual es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo") Integer stockActual,
        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo) {
}
