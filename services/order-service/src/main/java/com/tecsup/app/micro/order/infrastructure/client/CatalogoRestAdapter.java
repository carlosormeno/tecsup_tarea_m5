package com.tecsup.app.micro.order.infrastructure.client;

import com.tecsup.app.micro.order.domain.exception.CatalogoNoDisponibleException;
import com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException;
import com.tecsup.app.micro.order.domain.model.ProductoCatalogo;
import com.tecsup.app.micro.order.domain.client.CatalogoPort;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Adaptador de salida hacia catalog-service.
 *
 * Traduce los fallos HTTP a excepciones de dominio, y esa traducción es la que
 * decide después si el mensaje se reintenta o se va directo a la DLQ:
 *
 *   404 o producto no disponible -> determinista, no se reintenta
 *   timeout, 5xx, conexión caída -> transitorio, se reintenta
 *
 * CIRCUIT BREAKER. Aquí, y solo aquí, porque esta es la única llamada síncrona
 * del sistema (ADR-005). Sin él, con Catálogo caído cada petición esperaría los
 * 3 segundos del timeout antes de fallar, y bajo carga esas esperas se apilan
 * hasta agotar el pool de hilos de Pedidos: un servicio caído se llevaría por
 * delante a otro que está perfectamente. Con el circuito abierto, la llamada ni
 * se intenta y el fallo es inmediato.
 *
 * El circuito NO cuenta como fallo que un producto no exista: eso es una
 * respuesta correcta del catálogo a una pregunta mal hecha. Está declarado en
 * `ignore-exceptions` del application.yaml; si no lo estuviera, unos cuantos
 * productos inexistentes abrirían el circuito y tumbarían las compras válidas.
 */
@Slf4j
@Component
public class CatalogoRestAdapter implements CatalogoPort {

    private final RestClient restClient;

    public CatalogoRestAdapter(RestClient catalogoRestClient) {
        this.restClient = catalogoRestClient;
    }

    @Override
    @CircuitBreaker(name = "catalogo", fallbackMethod = "catalogoNoDisponible")
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
        }

        // Los fallos de transporte (timeout, 5xx, conexión caída) NO se atrapan
        // aquí a propósito: se dejan subir para que el circuito los cuente como
        // fallo. Quien los traduce a excepción de dominio es el método de
        // respaldo de abajo.
    }

    /**
     * Se ejecuta cuando el circuito está abierto o la llamada falló.
     *
     * No devuelve un producto inventado ni un precio por defecto, que sería lo
     * peor que podría hacer: un pedido con un precio falso es peor que un
     * pedido que no se crea. Traduce a la misma excepción de dominio de
     * siempre, para que quien llama no tenga que saber que existe un circuito.
     *
     * `ProductoNoDisponibleException` no llega aquí: está en `ignore-exceptions`
     * y se propaga tal cual.
     *
     * Visible en el paquete, no privado, para poder probarlo directamente.
     */
    ProductoCatalogo catalogoNoDisponible(Long productoId, Throwable causa) {
        if (causa instanceof CallNotPermittedException) {
            // Ni se intentó la llamada: el circuito está abierto por los fallos
            // anteriores. Esto es fallar rápido, no fallar más.
            log.warn("Circuito abierto: no se llama al catálogo por el producto {}", productoId);
            throw new CatalogoNoDisponibleException(
                    "circuito abierto tras fallos repetidos del catálogo", causa);
        }

        log.warn("Catálogo no respondió por el producto {}: {}", productoId, causa.getMessage());
        throw new CatalogoNoDisponibleException("GET /api/productos/" + productoId, causa);
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
