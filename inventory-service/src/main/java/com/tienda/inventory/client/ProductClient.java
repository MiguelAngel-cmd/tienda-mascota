package com.tienda.inventory.client;

import com.tienda.inventory.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente REST que consulta al Product-Service (comunicación Inventory -> Product). */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${product.service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    /** true si el producto existe en Product-Service; false si responde 404. */
    public boolean existe(Long productId) {
        try {
            restClient.get().uri("/products/{id}", productId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Product-Service no está disponible: " + e.getMessage());
        }
    }
}
