package com.tienda.inventory.exception;

/** Se lanza cuando se incumple una regla de negocio (HTTP 400). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
