package com.tecsup.app.micro.catalog.application;

import com.tecsup.app.micro.catalog.application.AjustarStockUseCase.ItemPedido;
import com.tecsup.app.micro.catalog.domain.exception.ProductoNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.exception.StockInsuficienteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * La idempotencia con tabla, que es lo que distingue a este servicio.
 *
 * En Pedidos y en Pagos repetir un evento es inofensivo. Aquí no: descontar
 * dos veces deja el inventario mal para siempre, y no hay forma de detectarlo
 * mirando el número.
 */
class AjustarStockUseCaseImplTest {

    private Fakes.FakeProductos productos;
    private Fakes.FakeEventosProcesados eventos;
    private AjustarStockUseCaseImpl ajustarStock;

    @BeforeEach
    void preparar() {
        productos = new Fakes.FakeProductos().conProducto(10L, 100).conProducto(20L, 50);
        eventos = new Fakes.FakeEventosProcesados();
        ajustarStock = new AjustarStockUseCaseImpl(productos, eventos);
    }

    private int stockDe(Long id) {
        return productos.buscarPorId(id).orElseThrow().getStock();
    }

    @Test
    @DisplayName("descuenta el stock de todos los items del pedido")
    void descuenta() {
        ajustarStock.descontar("evt-1", List.of(new ItemPedido(10L, 2), new ItemPedido(20L, 3)));

        assertThat(stockDe(10L)).isEqualTo(98);
        assertThat(stockDe(20L)).isEqualTo(47);
        assertThat(eventos.marcados).containsExactly("evt-1");
    }

    @Test
    @DisplayName("el MISMO evento repetido no descuenta dos veces")
    void idempotenciaAlDescontar() {
        ajustarStock.descontar("evt-1", List.of(new ItemPedido(10L, 2)));
        assertThat(stockDe(10L)).isEqualTo(98);

        // Kafka entrega al menos una vez: el mismo evento puede llegar de nuevo
        ajustarStock.descontar("evt-1", List.of(new ItemPedido(10L, 2)));

        assertThat(stockDe(10L)).isEqualTo(98);
        assertThat(eventos.marcados).containsExactly("evt-1");
    }

    @Test
    @DisplayName("dos eventos distintos sí descuentan los dos")
    void eventosDistintosSiSeAplican() {
        ajustarStock.descontar("evt-1", List.of(new ItemPedido(10L, 2)));
        ajustarStock.descontar("evt-2", List.of(new ItemPedido(10L, 3)));

        assertThat(stockDe(10L)).isEqualTo(95);
        assertThat(eventos.marcados).containsExactly("evt-1", "evt-2");
    }

    @Test
    @DisplayName("reponer devuelve las unidades")
    void repone() {
        ajustarStock.descontar("evt-1", List.of(new ItemPedido(10L, 5)));
        ajustarStock.reponer("evt-2", List.of(new ItemPedido(10L, 5)));

        assertThat(stockDe(10L)).isEqualTo(100);
    }

    @Test
    @DisplayName("una reposición repetida tampoco se aplica dos veces")
    void idempotenciaAlReponer() {
        ajustarStock.descontar("evt-1", List.of(new ItemPedido(10L, 5)));
        ajustarStock.reponer("evt-2", List.of(new ItemPedido(10L, 5)));
        ajustarStock.reponer("evt-2", List.of(new ItemPedido(10L, 5)));

        assertThat(stockDe(10L)).isEqualTo(100);
    }

    @Test
    @DisplayName("si el stock no alcanza, falla y NO marca el evento como procesado")
    void stockInsuficiente() {
        assertThatThrownBy(() -> ajustarStock.descontar("evt-1", List.of(new ItemPedido(20L, 999))))
                .isInstanceOf(StockInsuficienteException.class);

        // Clave: al no marcarse, un reintento posterior volvería a intentarlo.
        // Si se hubiera marcado antes de descontar, el ajuste se perdería.
        assertThat(eventos.marcados).isEmpty();
        assertThat(eventos.yaProcesado("evt-1")).isFalse();
    }

    @Test
    @DisplayName("un producto inexistente falla de forma determinista")
    void productoInexistente() {
        assertThatThrownBy(() -> ajustarStock.descontar("evt-1", List.of(new ItemPedido(999L, 1))))
                .isInstanceOf(ProductoNoEncontradoException.class);

        assertThat(eventos.marcados).isEmpty();
    }
}
