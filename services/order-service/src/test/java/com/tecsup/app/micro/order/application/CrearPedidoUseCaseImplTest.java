package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.application.CrearPedidoUseCase.ComandoCrearPedido;
import com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caso de uso de creación, sin Spring ni base de datos. */
class CrearPedidoUseCaseImplTest {

    private Fakes.FakeRepositorio repositorio;
    private CrearPedidoUseCaseImpl crearPedido;

    @BeforeEach
    void preparar() {
        repositorio = new Fakes.FakeRepositorio();
        crearPedido = new CrearPedidoUseCaseImpl(
                repositorio, new Fakes.FakeCatalogo().conProductosDePrueba());
    }

    @Test
    @DisplayName("consulta el catálogo, congela el precio y deja el pedido en CREADO")
    void crear() {
        Pedido pedido = crearPedido.crear(new ComandoCrearPedido(1L, "Av. Arequipa 123", List.of(
                new ComandoCrearPedido.ItemSolicitado(10L, 2),
                new ComandoCrearPedido.ItemSolicitado(20L, 3))));

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(pedido.total()).isEqualByComparingTo(new BigDecimal("86.80"));

        // El precio lo pone el catálogo, no el cliente
        assertThat(pedido.getLineas().get(0).precioUnitario()).isEqualByComparingTo("35.90");
        assertThat(pedido.getLineas().get(0).nombreProducto()).isEqualTo("Pizza margarita");

        assertThat(repositorio.buscarPorId(pedido.getId())).isPresent();
    }

    @Test
    @DisplayName("un producto no disponible impide crear el pedido")
    void productoNoDisponible() {
        assertThatThrownBy(() -> crearPedido.crear(new ComandoCrearPedido(1L, "Av. Arequipa 123",
                List.of(new ComandoCrearPedido.ItemSolicitado(99L, 1)))))
                .isInstanceOf(ProductoNoDisponibleException.class);

        assertThat(repositorio.buscarTodos()).isEmpty();
    }
}
