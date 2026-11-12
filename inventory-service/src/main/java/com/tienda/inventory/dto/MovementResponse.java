package com.tienda.inventory.dto;

import com.tienda.inventory.model.InventoryMovement;
import com.tienda.inventory.model.TipoMovimiento;
import java.time.LocalDateTime;

public record MovementResponse(Long id, Long productId, TipoMovimiento tipoMovimiento, Integer cantidad,
                               LocalDateTime fechaMovimiento) {

    public static MovementResponse from(InventoryMovement m) {
        return new MovementResponse(m.getId(), m.getProductId(), m.getTipoMovimiento(), m.getCantidad(),
                m.getFechaMovimiento());
    }
}
