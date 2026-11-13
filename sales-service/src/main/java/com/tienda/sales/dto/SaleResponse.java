package com.tienda.sales.dto;

import com.tienda.sales.model.EstadoVenta;
import com.tienda.sales.model.Sale;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(Long id, Long customerId, LocalDateTime fechaVenta, BigDecimal total,
                           EstadoVenta estado, List<SaleDetailResponse> detalles) {

    public static SaleResponse from(Sale s) {
        return new SaleResponse(s.getId(), s.getCustomerId(), s.getFechaVenta(), s.getTotal(), s.getEstado(),
                s.getDetalles().stream().map(SaleDetailResponse::from).toList());
    }
}
