package com.tienda.inventory.dto;

import com.tienda.inventory.model.TipoMovimiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * ENTRADA: suma "cantidad". SALIDA: resta "cantidad". AJUSTE: deja el stock EXACTAMENTE en "cantidad".
 */
public record MovementRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio") TipoMovimiento tipoMovimiento,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 0, message = "La cantidad no puede ser negativa") Integer cantidad) {
}
