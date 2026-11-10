package com.tienda.inventory.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Existencias actuales de UN producto (un registro por productId). */
@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referencia lógica al producto del otro microservicio (NO es una llave foránea real)
    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "stock_minimo", nullable = false)
    private Integer stockMinimo;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    public Inventory() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getStockActual() { return stockActual; }
    public void setStockActual(Integer stockActual) { this.stockActual = stockActual; }

    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
}
