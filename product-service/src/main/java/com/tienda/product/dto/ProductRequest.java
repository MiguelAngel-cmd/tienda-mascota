package com.tienda.product.dto;

import com.tienda.product.model.Categoria;
import com.tienda.product.model.EstadoProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Datos que el cliente envía para crear o actualizar un producto. */
public record ProductRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "La categoría es obligatoria") Categoria categoria,
        String marca,
        String descripcion,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio no puede ser negativo") BigDecimal precio,
        @NotBlank(message = "El código de barras es obligatorio") String codigoBarras,
        EstadoProducto estado) {
}
