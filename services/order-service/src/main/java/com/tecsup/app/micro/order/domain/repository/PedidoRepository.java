package com.tecsup.app.micro.order.domain.repository;

import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.model.Pedido;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida hacia la persistencia.
 *
 * Habla de Pedido, no de entidades JPA. La implementación vive en
 * infrastructure/adapter/out/persistence y es la única que sabe que detrás
 * hay Postgres.
 */
public interface PedidoRepository {

    Pedido guardar(Pedido pedido);

    Optional<Pedido> buscarPorId(UUID id);

    /**
     * Igual que buscarPorId pero fallando si no existe.
     *
     * Evita repetir el mismo orElseThrow en cada caso de uso. Vive aquí y no
     * en un ayudante de la capa de aplicación porque la excepción es de
     * dominio y el puerto también.
     */
    default Pedido obtener(UUID id) {
        return buscarPorId(id).orElseThrow(() -> new PedidoNoEncontradoException(id));
    }

    /** Los pedidos de un cliente, **del más reciente al más antiguo**. */
    List<Pedido> buscarPorCliente(Long clienteId);

    /** Todos los pedidos, con el mismo orden que buscarPorCliente. */
    List<Pedido> buscarTodos();
}
