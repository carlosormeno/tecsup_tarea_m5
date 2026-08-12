package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.event.EntregaEstadoCambiado;
import com.tecsup.app.micro.delivery.domain.exception.EntregaNoEncontradaException;
import com.tecsup.app.micro.delivery.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cada cambio de estado publica un evento, y es así como Pedidos avanza hasta
 * ENTREGADO. Si este servicio dejara de publicar, el pedido se quedaría
 * clavado en PAGADO.
 */
class ActualizarEntregaUseCaseImplTest {

    private Fakes.FakeEntregas entregas;
    private Fakes.FakePublicador publicador;
    private ActualizarEntregaUseCaseImpl actualizar;
    private Entrega entrega;

    @BeforeEach
    void preparar() {
        entregas = new Fakes.FakeEntregas();
        publicador = new Fakes.FakePublicador();
        actualizar = new ActualizarEntregaUseCaseImpl(entregas, publicador);

        entrega = new AsignarEntregaUseCaseImpl(
                entregas, new Fakes.FakeRepartidores().con(1L), publicador)
                .asignar(UUID.randomUUID(), 7L, "Av. Arequipa 123");

        publicador.limpiar();
    }

    @Test
    @DisplayName("cada avance publica su evento")
    void publicaCadaAvance() {
        actualizar.cambiarEstado(entrega.getId(), EstadoEntrega.EN_CAMINO, "en ruta");
        actualizar.cambiarEstado(entrega.getId(), EstadoEntrega.COMPLETADA, "entregado");

        assertThat(publicador.publicados).hasSize(2);
        assertThat(((EntregaEstadoCambiado) publicador.publicados.get(0)).nuevoEstado())
                .isEqualTo(EstadoEntrega.EN_CAMINO);
        assertThat(((EntregaEstadoCambiado) publicador.publicados.get(1)).nuevoEstado())
                .isEqualTo(EstadoEntrega.COMPLETADA);
    }

    @Test
    @DisplayName("repetir el mismo estado no republica el evento")
    void idempotencia() {
        actualizar.cambiarEstado(entrega.getId(), EstadoEntrega.EN_CAMINO, "en ruta");
        publicador.limpiar();

        actualizar.cambiarEstado(entrega.getId(), EstadoEntrega.EN_CAMINO, "en ruta otra vez");

        assertThat(publicador.publicados).isEmpty();
    }

    @Test
    @DisplayName("un salto inválido falla y no publica nada")
    void saltoInvalido() {
        assertThatThrownBy(() ->
                actualizar.cambiarEstado(entrega.getId(), EstadoEntrega.COMPLETADA, null))
                .isInstanceOf(TransicionInvalidaException.class);

        assertThat(publicador.publicados).isEmpty();
    }

    @Test
    @DisplayName("una entrega inexistente falla de forma determinista")
    void entregaInexistente() {
        assertThatThrownBy(() ->
                actualizar.cambiarEstado(UUID.randomUUID(), EstadoEntrega.EN_CAMINO, null))
                .isInstanceOf(EntregaNoEncontradaException.class);
    }
}
