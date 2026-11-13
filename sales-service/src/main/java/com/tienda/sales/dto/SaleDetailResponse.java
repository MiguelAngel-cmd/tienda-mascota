package com.tienda.sales.dto;

import com.tienda.sales.model.SaleDetail;
import java.math.BigDecimal;

public record SaleDetailResponse(Long id, Long productId, Integer cantidad, BigDecimal precioUnitario,
                                 BigDecimal subtotal) {

    public static SaleDetailResponse from(SaleDetail d) {
        return new SaleDetailResponse(d.getId(), d.getProductId(), d.getCantidad(), d.getPrecioUnitario(),
                d.getSubtotal());
    }
}
