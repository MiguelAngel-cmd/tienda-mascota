package com.tienda.sales.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tienda.sales.exception.BusinessRuleException;
import com.tienda.sales.exception.ServiceUnavailableException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Cliente REST que consulta y actualiza el Inventory-Service (comunicación Sales -> Inventory). */
@Component
public class InventoryClient {

    /** Cuerpo del POST /inventory/{productId}/movements. */
    record MovementPayload(String tipoMovimiento, Integer cantidad) { }

    /** Cuerpo de error que devuelven los servicios (solo interesa el mensaje). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RemoteError(String message) { }

    private final RestClient restClient;

    public InventoryClient(@Value("${inventory.service.url}") String baseUrl) {
        this.restClient = RestClients.crear(baseUrl);
    }

    /** Devuelve las existencias del producto o vacío si no tiene inventario (404). */
    public Optional<InventoryInfo> obtener(Long productId) {
        try {
            return Optional.ofNullable(
                    restClient.get().uri("/inventory/{productId}", productId).retrieve().body(InventoryInfo.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Inventory-Service no está disponible: " + e.getMessage());
        }
    }

    /** Descuenta stock (movimiento SALIDA). */
    public void descontar(Long productId, int cantidad) {
        mover(productId, "SALIDA", cantidad);
    }

    /** Devuelve stock (movimiento ENTRADA): se usa para deshacer una salida o cancelar una venta. */
    public void reponer(Long productId, int cantidad) {
        mover(productId, "ENTRADA", cantidad);
    }

    private void mover(Long productId, String tipo, int cantidad) {
        try {
            restClient.post().uri("/inventory/{productId}/movements", productId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new MovementPayload(tipo, cantidad))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException e) {
            throw new BusinessRuleException("Inventory-Service rechazó el movimiento " + tipo
                    + " del producto " + productId + ": " + mensaje(e));
        } catch (RestClientException e) {
            throw new ServiceUnavailableException("Inventory-Service no está disponible: " + e.getMessage());
        }
    }

    private String mensaje(HttpClientErrorException e) {
        try {
            RemoteError error = e.getResponseBodyAs(RemoteError.class);
            if (error != null && error.message() != null) {
                return error.message();
            }
        } catch (RuntimeException ignorada) {
            // si el cuerpo no se puede leer, se usa el texto del estado HTTP
        }
        return e.getStatusText();
    }
}
