package com.tienda.sales.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Existencias de un producto, según Inventory-Service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InventoryInfo(Long productId, Integer stockActual) {
}
