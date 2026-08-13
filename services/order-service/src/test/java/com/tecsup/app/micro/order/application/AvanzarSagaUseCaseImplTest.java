package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.application.CrearPedidoUseCase.ComandoCrearPedido;
import com.tecsup.app.micro.order.domain.event.PedidoCancelado;
import com.tecsup.app.micro.order.domain.event.PedidoConfirmado;
import com.tecsup.app.micro.order.domain.event.PedidoEntregado;
import com.tecsup.app.micro.order.domain.model.EstadoEntrega;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** La saga completa: reacciones a Pagos y a Entregas. */
class AvanzarSagaUseCaseImplTest {

    private Fakes.FakeRepositorio repositorio;
    private Fakes.FakePublicador publicador;
    private AvanzarSagaUseCaseImpl saga;
    private UUID pedidoId;

    @BeforeEach
    void preparar() {
        repositorio = new Fakes.FakeRepositorio();
        publicador = new Fakes.FakePublicador();
        saga = new AvanzarSagaUseCaseImpl(repositorio, publicador);

        Pedido pedido = new CrearPedidoUseCaseImpl(
                repositorio, new Fakes.FakeCatalogo().conProductosDePrueba())
                .crear(new ComandoCrearPedido(1L, "Av. Arequipa 123",
                        List.of(new ComandoCrearPedido.ItemSolicitado(10L, 2))));

        pedidoId = pedido.getId();

        // Todas estas pruebas parten de un pedido cuyo cobro ya se solicitó:
        // sin ese paso, Pagos no habría publicado nada a lo que reaccionar.
        new PagarPedidoUseCaseImpl(repositorio, publicador).pagar(pedidoId);
        publicador.limpiar();
    }

    private EstadoPedido estadoActual() {
        return repositorio.obtener(pedidoId).getEstado();
    }

    @Test
    @DisplayName("el pago confirmado pasa a PAGADO y publica pedido.confirmado")
    void pagoConfirmado() {
        saga.pagoConfirmado(pedidoId, "tx-001");

        assertThat(estadoActual()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(publicador.publicados).singleElement().isInstanceOf(PedidoConfirmado.class);
    }

    @Test
    @DisplayName("un pago.confirmado repetido se ignora y no vuelve a publicar")
    void idempotencia() {
        saga.pagoConfirmado(pedidoId, "tx-001");
        publicador.limpiar();

        // Kafka entrega al menos una vez: el mismo evento puede llegar dos veces
        saga.pagoConfirmado(pedidoId, "tx-001");

        assertThat(estadoActual()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(publicador.publicados).isEmpty();
    }

    @Test
    @DisplayName("el pago rechazado deja RECHAZADO y compensa sin reembolso")
    void pagoRechazado() {
        saga.pagoRechazado(pedidoId, "Saldo insuficiente");

        assertThat(estadoActual()).isEqualTo(EstadoPedido.RECHAZADO);

        PedidoCancelado cancelado = (PedidoCancelado) publicador.publicados.get(0);
        assertThat(cancelado.motivo()).isEqualTo("Saldo insuficiente");
        assertThat(cancelado.huboCobro()).isFalse();
    }

    @Test
    @DisplayName("la saga completa termina en ENTREGADO y publica pedido.entregado")
    void sagaCompleta() {
        saga.pagoConfirmado(pedidoId, "tx-001");
        saga.entregaCambioEstado(pedidoId, EstadoEntrega.ASIGNADA, "repartidor 7");
        assertThat(estadoActual()).isEqualTo(EstadoPedido.EN_PREPARACION);

        saga.entregaCambioEstado(pedidoId, EstadoEntrega.EN_CAMINO, null);
        assertThat(estadoActual()).isEqualTo(EstadoPedido.EN_CAMINO);

        publicador.limpiar();
        saga.entregaCambioEstado(pedidoId, EstadoEntrega.COMPLETADA, "entregado en puerta");

        assertThat(estadoActual()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(publicador.publicados).singleElement().isInstanceOf(PedidoEntregado.class);
    }

    @Test
    @DisplayName("una entrega fallida cancela el pedido indicando que hubo cobro")
    void entregaFallida() {
        saga.pagoConfirmado(pedidoId, "tx-001");
        publicador.limpiar();

        saga.entregaCambioEstado(pedidoId, EstadoEntrega.FALLIDA, "nadie en el domicilio");

        assertThat(estadoActual()).isEqualTo(EstadoPedido.CANCELADO);
        PedidoCancelado cancelado = (PedidoCancelado) publicador.publicados.get(0);
        assertThat(cancelado.huboCobro()).isTrue();
    }
}
