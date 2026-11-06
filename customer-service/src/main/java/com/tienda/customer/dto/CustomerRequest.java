package com.tienda.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Datos que el cliente envía para registrar o actualizar un cliente. */
public record CustomerRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String apellido,
        String documento,
        @NotBlank(message = "El teléfono es obligatorio") String telefono,
        // Formato texto@dominio.ext; si viene vacío se trata como "sin correo"
        @Pattern(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$",
                message = "El formato del correo electrónico no es válido") String correo,
        String direccion) {
}
