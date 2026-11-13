package com.tienda.sales.client;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Crea los clientes REST con tiempos de espera, para no quedarse colgado si otro servicio no responde. */
final class RestClients {

    private RestClients() { }

    static RestClient crear(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
