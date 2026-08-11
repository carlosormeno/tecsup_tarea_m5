package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.client.CatalogoPort;
import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.domain.event.EventoDominio;
import com.tecsup.app.micro.order.domain.exception.ProductoNoDisponibleException;
import com.tecsup.app.micro.order.domain.model.Pedido;
import com.tecsup.app.micro.order.domain.model.ProductoCatalogo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dobles de prueba de los puertos de salida.
 *
 * Escritos a mano y no con Mockito: los puertos son interfaces de tres o
 * cuatro métodos, y una implementación en memoria se lee mejor que una pila de
 * `when(...).thenReturn(...)`. Que esto sea tan barato es precisamente lo que
 * demuestra que el hexágono está bien cerrado.
 */
public final class Fakes {

    private Fakes() {
    }

    public static class FakeRepositorio implements PedidoRepository {
        private final Map<UUID, Pedido> datos = new HashMap<>();

        @Override
        public Pedido guardar(Pedido pedido) {
            datos.put(pedido.getId(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public List<Pedido> buscarPorCliente(Long clienteId) {
            return datos.values().stream().filter(p -> p.getClienteId().equals(clienteId)).toList();
        }

        @Override
        public List<Pedido> buscarTodos() {
            return List.copyOf(datos.values());
        }
    }

    public static class FakePublicador implements PublicadorEventos {
        public final List<EventoDominio> publicados = new ArrayList<>();

        @Override
        public void publicar(EventoDominio evento) {
            publicados.add(evento);
        }

        public void limpiar() {
            publicados.clear();
        }
    }

    public static class FakeCatalogo implements CatalogoPort {
        private final Map<Long, ProductoCatalogo> productos = new HashMap<>();

        public FakeCatalogo conProductosDePrueba() {
            registrar(new ProductoCatalogo(10L, "Pizza margarita", new BigDecimal("35.90"), true));
            registrar(new ProductoCatalogo(20L, "Gaseosa 500ml", new BigDecimal("5.00"), true));
            registrar(new ProductoCatalogo(99L, "Postre agotado", new BigDecimal("12.00"), false));
            return this;
        }

        public void registrar(ProductoCatalogo producto) {
            productos.put(producto.id(), producto);
        }

        @Override
        public ProductoCatalogo obtenerProducto(Long productoId) {
            ProductoCatalogo producto = productos.get(productoId);
            if (producto == null || !producto.disponible()) {
                throw new ProductoNoDisponibleException(productoId);
            }
            return producto;
        }
    }
}
