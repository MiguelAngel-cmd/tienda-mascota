package com.tienda.sales.exception;

/** Se lanza cuando otro microservicio no responde (HTTP 503). */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) { super(message); }
}
