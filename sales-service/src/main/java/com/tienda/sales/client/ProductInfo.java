package com.tienda.sales.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** Solo los datos del producto que Sales necesita (el resto del JSON se ignora). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfo(Long id, String nombre, BigDecimal precio, String estado) {
}
