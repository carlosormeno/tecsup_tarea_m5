package com.tecsup.app.micro.order.domain.model;

import com.tecsup.app.micro.order.domain.exception.TransicionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del agregado. Sin Spring, sin base de datos, sin Kafka: el dominio
 * es una clase Java corriente y se prueba en milisegundos. Ese es el beneficio
 * concreto de la arquitectura hexagonal, no un argumento teórico.
 */
class PedidoTest {

    private static final Long CLIENTE = 1L;
    private static final String DIRECCION = "Av. Arequipa 123";

    private Pedido unPedido() {
        return Pedido.crear(CLIENTE, DIRECCION, List.of(
                new LineaPedido(10L, "Pizza margarita", new BigDecimal("35.90"), 2),
                new LineaPedido(20L, "Gaseosa 500ml", new BigDecimal("5.00"), 3)));
    }

    @Test
    @DisplayName("un pedido nuevo nace en CREADO y calcula su total")
    void pedidoNuevo() {
        Pedido pedido = unPedido();

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(pedido.getId()).isNotNull();
        // 35.90 * 2 + 5.00 * 3 = 86.80
        assertThat(pedido.total()).isEqualByComparingTo(new BigDecimal("86.80"));
    }

    private Pedido unPedidoEntregado() {
        Pedido pedido = unPedido();
        pedido.transicionarA(EstadoPedido.PAGO_EN_PROCESO, null);
        pedido.transicionarA(EstadoPedido.PAGADO, "pago-123");
        pedido.transicionarA(EstadoPedido.EN_PREPARACION, null);
        pedido.transicionarA(EstadoPedido.EN_CAMINO, null);
        pedido.transicionarA(EstadoPedido.ENTREGADO, null);
        return pedido;
    }

    @Test
    @DisplayName("el camino feliz recorre toda la máquina de estados")
    void caminoFeliz() {
        Pedido pedido = unPedidoEntregado();

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(pedido.getEstado().esFinal()).isTrue();
    }

    @Test
    @DisplayName("no se puede saltar de CREADO a ENTREGADO")
    void noPermiteSaltos() {
        Pedido pedido = unPedido();

        assertThatThrownBy(() -> pedido.transicionarA(EstadoPedido.ENTREGADO, null))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining("no puede pasar de CREADO a ENTREGADO");
    }

    @Test
    @DisplayName("un pedido no puede darse por pagado sin haber pasado por el cobro")
    void exigePasarPorElCobro() {
        Pedido pedido = unPedido();

        // Esta es la regla que hace que el botón de pagar no sea decorativo:
        // el único camino a PAGADO pasa por PAGO_EN_PROCESO, y a ese estado
        // solo se llega solicitando el cobro.
        assertThatThrownBy(() -> pedido.transicionarA(EstadoPedido.PAGADO, "pago-123"))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("pagar dos veces el mismo pedido no es una transición válida")
    void noSePagaDosVeces() {
        Pedido pedido = unPedido();
        pedido.transicionarA(EstadoPedido.PAGO_EN_PROCESO, null);

        // El doble clic en «pagar» muere aquí, en el dominio, no en el navegador
        assertThatThrownBy(() -> pedido.transicionarA(EstadoPedido.PAGO_EN_PROCESO, null))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("solo a partir de PAGADO hay dinero que devolver")
    void implicaCobro() {
        assertThat(EstadoPedido.CREADO.implicaCobro()).isFalse();
        // El cobro puede estar en vuelo: se trata como «todavía no»
        assertThat(EstadoPedido.PAGO_EN_PROCESO.implicaCobro()).isFalse();
        assertThat(EstadoPedido.PAGADO.implicaCobro()).isTrue();
        assertThat(EstadoPedido.EN_CAMINO.implicaCobro()).isTrue();
    }

    @Test
    @DisplayName("un pedido entregado ya no admite cambios")
    void estadoFinalEsFinal() {
        Pedido pedido = unPedidoEntregado();

        assertThatThrownBy(() -> pedido.transicionarA(EstadoPedido.CANCELADO, "tarde"))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("un pedido sin líneas no es válido")
    void exigeLineas() {
        assertThatThrownBy(() -> Pedido.crear(CLIENTE, DIRECCION, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una línea");
    }

    @Test
    @DisplayName("una línea con cantidad cero no es válida")
    void exigeCantidadPositiva() {
        assertThatThrownBy(() -> new LineaPedido(10L, "Pizza", new BigDecimal("35.90"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mayor que cero");
    }
}
