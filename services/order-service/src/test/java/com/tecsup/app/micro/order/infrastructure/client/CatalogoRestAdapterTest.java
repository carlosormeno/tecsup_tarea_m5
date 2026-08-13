package com.tecsup.app.micro.order.infrastructure.client;

import com.tecsup.app.micro.order.domain.exception.CatalogoNoDisponibleException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lo que hace el adaptador cuando el catálogo no responde.
 *
 * Se prueba el método de respaldo directamente, sin levantar Spring ni un
 * servidor: lo que importa aquí es la traducción a excepción de dominio, no la
 * mecánica del circuito, que es de la librería y ya está probada por ella.
 */
class CatalogoRestAdapterTest {

    private final CatalogoRestAdapter adaptador = new CatalogoRestAdapter(null);

    @Test
    @DisplayName("un fallo de transporte se traduce a excepción de dominio")
    void falloDeTransporte() {
        Throwable causa = new ResourceAccessException(
                "I/O error", new SocketTimeoutException("Read timed out"));

        // Quien llama no debe enterarse de que detrás hay HTTP: recibe la
        // excepción de dominio, que el manejador global convierte en 503.
        assertThatThrownBy(() -> adaptador.catalogoNoDisponible(10L, causa))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageContaining("10");
    }

    @Test
    @DisplayName("con el circuito abierto falla rápido y lo dice")
    void circuitoAbierto() {
        CircuitBreaker circuito = CircuitBreaker.ofDefaults("catalogo");
        circuito.transitionToOpenState();

        Throwable causa = CallNotPermittedException.createCallNotPermittedException(circuito);

        // El mensaje distingue "no respondió" de "ni lo intenté": es la
        // diferencia entre esperar 3 s y fallar al instante, y quien lea el log
        // necesita saber cuál de las dos ocurrió.
        assertThatThrownBy(() -> adaptador.catalogoNoDisponible(10L, causa))
                .isInstanceOf(CatalogoNoDisponibleException.class)
                .hasMessageContaining("circuito abierto");
    }
}
