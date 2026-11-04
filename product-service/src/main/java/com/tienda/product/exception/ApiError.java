package com.tienda.product.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Formato uniforme de error que devuelve la API. */
public record ApiError(LocalDateTime timestamp, int status, String error, String message, List<String> details) {
}
