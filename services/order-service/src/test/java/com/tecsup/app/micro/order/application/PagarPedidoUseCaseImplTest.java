package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.application.CrearPedidoUseCase.ComandoCrearPedido;
import com.tecsup.app.micro.order.domain.event.PagoSolicitado;
import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * El paso que arranca la saga.
 *
 * Crear un pedido no cobra nada; esto es lo que lo cobra. Que sean dos casos de
 * uso separados es lo que permite que un pedido exista sin haberse pagado.
 */
class PagarPedidoUseCaseImplTest {

    private Fakes.FakeRepositorio repositorio;
    private Fakes.FakePublicador publicador;
    private PagarPedidoUseCaseImpl pagar;
    private UUID pedidoId;

    @BeforeEach
    void preparar() {
        repositorio = new Fakes.FakeRepositorio();
        publicador = new Fakes.FakePublicador();
        pagar = new PagarPedidoUseCaseImpl(repositorio, publicador);

        Pedido pedido = new CrearPedidoUseCaseImpl(
                repositorio, new Fakes.FakeCatalogo().conProductosDePrueba())
                .crear(new ComandoCrearPedido(1L, "Av. Arequipa 123",
                        List.of(new ComandoCrearPedido.ItemSolicitado(10L, 2))));

        pedidoId = pedido.getId();
    }

    @Test
    @DisplayName("crear un pedido no publica nada: la saga no ha arrancado")
    void crearNoArrancaLaSaga() {
        assertThat(repositorio.obtener(pedidoId).getEstado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(publicador.publicados).isEmpty();
    }

    @Test
    @DisplayName("pagar deja el pedido en PAGO_EN_PROCESO y publica pedido.pago-solicitado")
    void pagar() {
        Pedido pedido = pagar.pagar(pedidoId);

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PAGO_EN_PROCESO);

        assertThat(publicador.publicados).singleElement().isInstanceOf(PagoSolicitado.class);
        PagoSolicitado evento = (PagoSolicitado) publicador.publicados.get(0);
        assertThat(evento.pedidoId()).isEqualTo(pedidoId);
        // El importe que se cobra es el congelado al crear, no uno que llegue
        // desde el cliente: 35.90 * 2
        assertThat(evento.total()).isEqualByComparingTo(new BigDecimal("71.80"));
        assertThat(evento.idAgregado()).isEqualTo(pedidoId.toString());
    }

    @Test
    @DisplayName("un segundo intento de pago falla y no publica un segundo evento")
    void noSePagaDosVeces() {
        pagar.pagar(pedidoId);
        publicador.limpiar();

        // El doble clic en el botón: la máquina de estados lo corta en seco.
        // Sin esto, Pagos recibiría dos eventos y solo lo salvaría el UNIQUE
        // de su tabla.
        assertThatThrownBy(() -> pagar.pagar(pedidoId))
                .isInstanceOf(TransicionInvalidaException.class);

        assertThat(publicador.publicados).isEmpty();
    }

    @Test
    @DisplayName("pagar un pedido inexistente falla con la excepción de dominio")
    void pedidoInexistente() {
        assertThatThrownBy(() -> pagar.pagar(UUID.randomUUID()))
                .isInstanceOf(PedidoNoEncontradoException.class);

        assertThat(publicador.publicados).isEmpty();
    }
}
