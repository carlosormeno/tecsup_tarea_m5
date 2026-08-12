package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.event.EntregaEstadoCambiado;
import com.tecsup.app.micro.delivery.domain.exception.SinRepartidoresException;
import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AsignarEntregaUseCaseImplTest {

    private Fakes.FakeEntregas entregas;
    private Fakes.FakePublicador publicador;

    @BeforeEach
    void preparar() {
        entregas = new Fakes.FakeEntregas();
        publicador = new Fakes.FakePublicador();
    }

    private AsignarEntregaUseCaseImpl conRepartidores(Long... ids) {
        return new AsignarEntregaUseCaseImpl(
                entregas, new Fakes.FakeRepartidores().con(ids), publicador);
    }

    @Test
    @DisplayName("asigna repartidor y publica el estado ASIGNADA")
    void asigna() {
        UUID pedidoId = UUID.randomUUID();

        Entrega entrega = conRepartidores(1L).asignar(pedidoId, 7L, "Av. Arequipa 123");

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entrega.getRepartidorId()).isEqualTo(1L);

        assertThat(publicador.publicados).hasSize(1);
        EntregaEstadoCambiado evento = (EntregaEstadoCambiado) publicador.publicados.get(0);
        assertThat(evento.pedidoId()).isEqualTo(pedidoId);
        assertThat(evento.nuevoEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        // La clave de partición es el pedido, no la entrega
        assertThat(evento.idAgregado()).isEqualTo(pedidoId.toString());
    }

    @Test
    @DisplayName("un pedido.confirmado duplicado no crea una segunda entrega")
    void idempotencia() {
        UUID pedidoId = UUID.randomUUID();
        AsignarEntregaUseCaseImpl asignar = conRepartidores(1L, 2L);

        asignar.asignar(pedidoId, 7L, "Av. Arequipa 123");
        publicador.limpiar();

        // Kafka entrega al menos una vez: sin este filtro, dos repartidores
        // quedarían ocupados con el mismo pedido.
        asignar.asignar(pedidoId, 7L, "Av. Arequipa 123");

        assertThat(entregas.buscarTodas()).hasSize(1);
        assertThat(publicador.publicados).isEmpty();
    }

    @Test
    @DisplayName("reparte la carga: elige al repartidor con menos entregas en curso")
    void repartoDeCarga() {
        AsignarEntregaUseCaseImpl asignar = conRepartidores(1L, 2L, 3L);

        Entrega primera = asignar.asignar(UUID.randomUUID(), 1L, "Dirección 1");
        Entrega segunda = asignar.asignar(UUID.randomUUID(), 2L, "Dirección 2");
        Entrega tercera = asignar.asignar(UUID.randomUUID(), 3L, "Dirección 3");

        // Cada una a un repartidor distinto en vez de las tres al primero
        assertThat(List.of(primera.getRepartidorId(), segunda.getRepartidorId(),
                tercera.getRepartidorId())).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("una entrega terminada libera al repartidor")
    void entregaTerminadaLibera() {
        AsignarEntregaUseCaseImpl asignar = conRepartidores(1L, 2L);
        var actualizar = new ActualizarEntregaUseCaseImpl(entregas, publicador);

        Entrega primera = asignar.asignar(UUID.randomUUID(), 1L, "Dirección 1");
        actualizar.cambiarEstado(primera.getId(), EstadoEntrega.EN_CAMINO, null);
        actualizar.cambiarEstado(primera.getId(), EstadoEntrega.COMPLETADA, null);

        // El repartidor 1 ya no tiene nada en curso, así que vuelve a estar
        // tan libre como el 2 y puede recibir la siguiente.
        assertThat(entregas.contarEnCursoDe(1L)).isZero();
    }

    @Test
    @DisplayName("sin repartidores activos falla de forma transitoria")
    void sinRepartidores() {
        assertThatThrownBy(() ->
                conRepartidores().asignar(UUID.randomUUID(), 1L, "Av. Arequipa 123"))
                .isInstanceOf(SinRepartidoresException.class);

        // No se publica nada: la saga se detiene y el evento se reintentará
        assertThat(publicador.publicados).isEmpty();
    }

}
