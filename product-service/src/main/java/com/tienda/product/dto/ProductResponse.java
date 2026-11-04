package com.tienda.product.dto;

import com.tienda.product.model.Categoria;
import com.tienda.product.model.EstadoProducto;
import com.tienda.product.model.Product;
import java.math.BigDecimal;

/** Datos que la API devuelve (nunca se expone la entidad directamente). */
public record ProductResponse(Long id, String nombre, Categoria categoria, String marca, String descripcion,
                              BigDecimal precio, String codigoBarras, EstadoProducto estado) {

    public static ProductResponse from(Product p) {
        return new ProductResponse(p.getId(), p.getNombre(), p.getCategoria(), p.getMarca(),
                p.getDescripcion(), p.getPrecio(), p.getCodigoBarras(), p.getEstado());
    }
}
