package com.tienda.customer.dto;

import com.tienda.customer.model.Customer;
import java.time.LocalDateTime;

/** Datos que la API devuelve (nunca se expone la entidad directamente). */
public record CustomerResponse(Long id, String nombre, String apellido, String documento, String telefono,
                               String correo, String direccion, LocalDateTime fechaRegistro) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getNombre(), c.getApellido(), c.getDocumento(),
                c.getTelefono(), c.getCorreo(), c.getDireccion(), c.getFechaRegistro());
    }
}
