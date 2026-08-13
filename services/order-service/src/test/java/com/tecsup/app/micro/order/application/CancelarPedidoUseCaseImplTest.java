package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.application.CrearPedidoUseCase.ComandoCrearPedido;
import com.tecsup.app.micro.order.domain.event.PedidoCancelado;
import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cancelación pedida por el cliente. */
class CancelarPedidoUseCaseImplTest {

    private Fakes.FakeRepositorio repositorio;
    private Fakes.FakePublicador publicador;
    private CancelarPedidoUseCaseImpl cancelar;
    private PagarPedidoUseCaseImpl pagar;
    private AvanzarSagaUseCaseImpl saga;
    private UUID pedidoId;

    @BeforeEach
    void preparar() {
        repositorio = new Fakes.FakeRepositorio();
        publicador = new Fakes.FakePublicador();
        cancelar = new CancelarPedidoUseCaseImpl(repositorio, publicador);
        pagar = new PagarPedidoUseCaseImpl(repositorio, publicador);
        saga = new AvanzarSagaUseCaseImpl(repositorio, publicador);

        Pedido pedido = new CrearPedidoUseCaseImpl(
                repositorio, new Fakes.FakeCatalogo().conProductosDePrueba())
                .crear(new ComandoCrearPedido(1L, "Av. Arequipa 123",
                        List.of(new ComandoCrearPedido.ItemSolicitado(10L, 2))));

        pedidoId = pedido.getId();
        publicador.limpiar();
    }

    @Test
    @DisplayName("cancelar antes de pagar no genera reembolso")
    void cancelarSinCobro() {
        cancelar.cancelar(pedidoId, "Me equivoqué de dirección");

        assertThat(repositorio.obtener(pedidoId).getEstado()).isEqualTo(EstadoPedido.CANCELADO);

        PedidoCancelado evento = (PedidoCancelado) publicador.publicados.get(0);
        assertThat(evento.huboCobro()).isFalse();
    }

    @Test
    @DisplayName("cancelar mientras el cobro está en vuelo tampoco pide reembolso")
    void cancelarConElPagoEnProceso() {
        pagar.pagar(pedidoId);
        publicador.limpiar();

        cancelar.cancelar(pedidoId, "Me arrepentí a medio pagar");

        // Todavía no hay dinero cobrado. Si el cobro terminara igualmente, el
        // reembolso de Pagos tolera que no haya pago que devolver.
        PedidoCancelado evento = (PedidoCancelado) publicador.publicados.get(0);
        assertThat(evento.huboCobro()).isFalse();
    }

    @Test
    @DisplayName("cancelar después de pagar marca que hay que reembolsar")
    void cancelarConCobro() {
        pagar.pagar(pedidoId);
        saga.pagoConfirmado(pedidoId, "tx-001");
        publicador.limpiar();

        cancelar.cancelar(pedidoId, "El cliente se arrepintió");

        PedidoCancelado evento = (PedidoCancelado) publicador.publicados.get(0);
        assertThat(evento.huboCobro()).isTrue();
    }

    @Test
    @DisplayName("cancelar un pedido inexistente falla con la excepción de dominio")
    void pedidoInexistente() {
        assertThatThrownBy(() -> cancelar.cancelar(UUID.randomUUID(), "da igual"))
                .isInstanceOf(PedidoNoEncontradoException.class);
    }
}
