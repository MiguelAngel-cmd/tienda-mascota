package com.tienda.sales.client;

import com.tienda.sales.exception.ServiceUnavailableException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente REST que consulta al Product-Service (comunicación Sales -> Product). */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(@Value("${product.service.url}") String baseUrl) {
        this.restClient = RestClients.crear(baseUrl);
    }

    /** Devuelve el producto (con su precio y estado) o vacío si Product-Service responde 404. */
    public Optional<ProductInfo> obtener(Long productId) {
        try {
            return Optional.ofNullable(
                    restClient.get().uri("/products/{id}", productId).retrieve().body(ProductInfo.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Product-Service no está disponible: " + e.getMessage());
        }
    }
}
