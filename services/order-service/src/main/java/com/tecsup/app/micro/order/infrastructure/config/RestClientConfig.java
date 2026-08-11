package com.tecsup.app.micro.order.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient catalogoRestClient(
            @Value("${catalogo.service.url:http://localhost:8082}") String urlBase) {

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(3));

        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabrica)
                .requestInterceptor(propagarJwt())
                .build();
    }

    private ClientHttpRequestInterceptor propagarJwt() {
        return (peticion, cuerpo, ejecucion) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
                String autorizacion = atributos.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

                if (autorizacion != null && !autorizacion.isBlank()) {
                    peticion.getHeaders().set(HttpHeaders.AUTHORIZATION, autorizacion);
                }
            }
            return ejecucion.execute(peticion, cuerpo);
        };
    }
}
