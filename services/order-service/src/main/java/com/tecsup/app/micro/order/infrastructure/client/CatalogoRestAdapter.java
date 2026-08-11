package com.tecsup.app.micro.order.infrastructure.client;

import com.tecsup.app.micro.order.domain.exception.CatalogoNoDisponibleException;
import com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException;
import com.tecsup.app.micro.order.domain.model.ProductoCatalogo;
import com.tecsup.app.micro.order.domain.client.CatalogoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

/**
 * Adaptador de salida hacia catalog-service.
 *
 * Traduce los fallos HTTP a excepciones de dominio, y esa traducción es la que
 * decide después si el mensaje se reintenta o se va directo a la DLQ:
 *
 *   404 o producto no disponible -> determinista, no se reintenta
 *   timeout, 5xx, conexión caída -> transitorio, se reintenta
 */
@Slf4j
@Component
public class CatalogoRestAdapter implements CatalogoPort {

    private final RestClient restClient;

    public CatalogoRestAdapter(RestClient catalogoRestClient) {
        this.restClient = catalogoRestClient;
    }

    @Override
    public ProductoCatalogo obtenerProducto(Long productoId) {
        try {
            ProductoRespuesta respuesta = restClient.get()
                    .uri("/api/productos/{id}", productoId)
                    .retrieve()
                    .body(ProductoRespuesta.class);

            if (respuesta == null || !respuesta.disponible()) {
                throw new ProductoNoDisponibleException(productoId);
            }

            return new ProductoCatalogo(
                    respuesta.id(), respuesta.nombre(), respuesta.precio(), respuesta.disponible());

        } catch (HttpClientErrorException.NotFound e) {
            throw new ProductoNoDisponibleException(productoId);

        } catch (RestClientException e) {
            log.warn("Catálogo no respondió por el producto {}: {}", productoId, e.getMessage());
            throw new CatalogoNoDisponibleException("GET /api/productos/" + productoId, e);
        }
    }

    /** Forma en la que catalog-service devuelve un producto. */
    private record ProductoRespuesta(
            Long id,
            String nombre,
            BigDecimal precio,
            boolean disponible
    ) {
    }
}
