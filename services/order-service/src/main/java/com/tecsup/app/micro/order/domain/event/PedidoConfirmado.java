package com.tecsup.app.micro.order.domain.event;

import com.tecsup.app.micro.order.domain.model.LineaPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El pago salió bien. Tiene dos consumidores con necesidades distintas:
 * Entregas usa la dirección para asignar repartidor, y Catálogo usa los items
 * para descontar stock.
 */
public record PedidoConfirmado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        String direccionEntrega,
        List<ItemEvento> items
) implements EventoDominio {

    public static PedidoConfirmado de(Pedido pedido) {
        List<ItemEvento> items = pedido.getLineas().stream()
                .map(l -> new ItemEvento(l.productoId(), l.cantidad()))
                .toList();

        return new PedidoConfirmado(
                UUID.randomUUID().toString(),
                Instant.now(),
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getDireccionEntrega(),
                items);
    }


    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
