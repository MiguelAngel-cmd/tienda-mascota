package com.tienda.sales.client;

import com.tienda.sales.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente REST que consulta al Customer-Service (comunicación Sales -> Customer). */
@Component
public class CustomerClient {

    private final RestClient restClient;

    public CustomerClient(@Value("${customer.service.url}") String baseUrl) {
        this.restClient = RestClients.crear(baseUrl);
    }

    /** true si el cliente existe en Customer-Service; false si responde 404. */
    public boolean existe(Long customerId) {
        try {
            restClient.get().uri("/customers/{id}", customerId).retrieve().toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Customer-Service no está disponible: " + e.getMessage());
        }
    }
}
