package com.tienda.inventory.exception;

/** Se lanza cuando un recurso no existe (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}
