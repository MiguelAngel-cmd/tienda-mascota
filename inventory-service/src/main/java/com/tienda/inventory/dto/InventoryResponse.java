package com.tienda.inventory.dto;

import com.tienda.inventory.model.Inventory;
import java.time.LocalDateTime;

public record InventoryResponse(Long id, Long productId, Integer stockActual, Integer stockMinimo,
                                LocalDateTime ultimaActualizacion, boolean alertaStockBajo) {

    public static InventoryResponse from(Inventory i) {
        return new InventoryResponse(i.getId(), i.getProductId(), i.getStockActual(), i.getStockMinimo(),
                i.getUltimaActualizacion(), i.getStockActual() < i.getStockMinimo());
    }
}
