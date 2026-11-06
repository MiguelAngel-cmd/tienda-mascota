package com.tienda.customer.exception;

/** Se lanza cuando se viola una restricción de unicidad (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}
