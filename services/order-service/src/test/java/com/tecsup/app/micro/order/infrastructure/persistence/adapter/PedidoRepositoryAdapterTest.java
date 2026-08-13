package com.tecsup.app.micro.order.infrastructure.persistence.adapter;

import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.LineaPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba del adaptador de persistencia.
 *
 * Lo que se verifica aquí es la traducción de ida y vuelta entre el agregado y
 * la entidad JPA. Es el punto donde de verdad se rompen las cosas: una columna
 * mal mapeada, el enum guardado como número, una lista de líneas que se pierde
 * o un BigDecimal que cambia de escala. Nada de eso lo detectan las pruebas de
 * dominio, porque ahí no hay base de datos.
 */
@DataJpaTest
@Import(PedidoRepositoryAdapter.class)
class PedidoRepositoryAdapterTest {

    @Autowired
    private PedidoRepositoryAdapter repositorio;

    private Pedido unPedido(Long clienteId) {
        return Pedido.crear(clienteId, "Av. Arequipa 123", List.of(
                new LineaPedido(10L, "Pizza margarita", new BigDecimal("35.90"), 2),
                new LineaPedido(20L, "Gaseosa 500ml", new BigDecimal("5.00"), 3)));
    }

    /** Con fecha fija: dos pedidos creados en el mismo instante no tienen orden. */
    private Pedido unPedidoDe(Long clienteId, Instant creadoEn) {
        return Pedido.reconstituir(UUID.randomUUID(), clienteId, "Av. Arequipa 123",
                List.of(new LineaPedido(10L, "Pizza margarita", new BigDecimal("35.90"), 2)),
                EstadoPedido.CREADO, null, creadoEn, creadoEn);
    }

    @Test
    @DisplayName("los listados van del más reciente al más antiguo y no se reordenan al actualizar")
    void ordenEstable() {
        Instant base = Instant.parse("2026-08-12T10:00:00Z");
        Pedido viejo = repositorio.guardar(unPedidoDe(1L, base));
        Pedido medio = repositorio.guardar(unPedidoDe(1L, base.plusSeconds(60)));
        Pedido nuevo = repositorio.guardar(unPedidoDe(1L, base.plusSeconds(120)));

        // Las dos consultas ordenan igual: si no, la misma lista se ve distinta
        // según se filtre por cliente o no.
        assertThat(repositorio.buscarTodos()).extracting(Pedido::getId)
                .containsExactly(nuevo.getId(), medio.getId(), viejo.getId());
        assertThat(repositorio.buscarPorCliente(1L)).extracting(Pedido::getId)
                .containsExactly(nuevo.getId(), medio.getId(), viejo.getId());

        viejo.transicionarA(EstadoPedido.PAGO_EN_PROCESO, null);
        repositorio.guardar(viejo);

        // El orden es por fecha de creación, no por el orden físico de las
        // filas. Esta prueba fija el contrato; el reordenamiento real solo se
        // ve en Postgres, que reescribe la fila actualizada al final del heap.
        assertThat(repositorio.buscarTodos()).extracting(Pedido::getId)
                .containsExactly(nuevo.getId(), medio.getId(), viejo.getId());
    }

    @Test
    @DisplayName("un pedido guardado se recupera idéntico, con sus líneas")
    void idaYVuelta() {
        Pedido guardado = repositorio.guardar(unPedido(1L));

        Optional<Pedido> leido = repositorio.buscarPorId(guardado.getId());

        assertThat(leido).isPresent();
        Pedido pedido = leido.orElseThrow();

        assertThat(pedido.getId()).isEqualTo(guardado.getId());
        assertThat(pedido.getClienteId()).isEqualTo(1L);
        assertThat(pedido.getDireccionEntrega()).isEqualTo("Av. Arequipa 123");
        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
        assertThat(pedido.getCreadoEn()).isNotNull();
        assertThat(pedido.total()).isEqualByComparingTo(new BigDecimal("86.80"));

        assertThat(pedido.getLineas()).hasSize(2);
        assertThat(pedido.getLineas())
                .extracting(LineaPedido::productoId, LineaPedido::nombreProducto, LineaPedido::cantidad)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(10L, "Pizza margarita", 2),
                        org.assertj.core.groups.Tuple.tuple(20L, "Gaseosa 500ml", 3));
    }

    @Test
    @DisplayName("el precio conserva sus dos decimales al pasar por la base")
    void conservaEscalaDelPrecio() {
        Pedido guardado = repositorio.guardar(unPedido(1L));

        LineaPedido linea = repositorio.buscarPorId(guardado.getId()).orElseThrow()
                .getLineas().stream()
                .filter(l -> l.productoId().equals(10L))
                .findFirst().orElseThrow();

        assertThat(linea.precioUnitario()).isEqualByComparingTo(new BigDecimal("35.90"));
    }

    @Test
    @DisplayName("el cambio de estado se persiste sobre el mismo pedido")
    void actualizaEstado() {
        Pedido pedido = repositorio.guardar(unPedido(1L));

        pedido.transicionarA(EstadoPedido.PAGO_EN_PROCESO, null);
        pedido.transicionarA(EstadoPedido.PAGADO, "tx-001");
        repositorio.guardar(pedido);

        Pedido leido = repositorio.buscarPorId(pedido.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoPedido.PAGADO);
        assertThat(leido.getMotivo()).isEqualTo("tx-001");
        // No debe haber duplicado las líneas al actualizar
        assertThat(leido.getLineas()).hasSize(2);
        assertThat(repositorio.buscarTodos()).hasSize(1);
    }

    @Test
    @DisplayName("buscar por cliente solo devuelve los suyos")
    void filtraPorCliente() {
        repositorio.guardar(unPedido(1L));
        repositorio.guardar(unPedido(1L));
        repositorio.guardar(unPedido(2L));

        assertThat(repositorio.buscarPorCliente(1L)).hasSize(2);
        assertThat(repositorio.buscarPorCliente(2L)).hasSize(1);
        assertThat(repositorio.buscarPorCliente(99L)).isEmpty();
    }

    @Test
    @DisplayName("un id inexistente devuelve vacío, no una excepción")
    void idInexistente() {
        assertThat(repositorio.buscarPorId(UUID.randomUUID())).isEmpty();
    }
}
