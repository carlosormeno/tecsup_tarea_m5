package com.tecsup.app.micro.order.domain.event;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Evento de compensación de la saga.
 *
 * Lo consumen Pagos (para reembolsar, si ya se había cobrado) y Catálogo (para
 * reponer el stock que se había descontado).
 */
public record PedidoCancelado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        String motivo,
        boolean huboCobro,
        List<ItemEvento> items
) implements EventoDominio {

    public static PedidoCancelado de(Pedido pedido, String motivo, boolean huboCobro) {
        List<ItemEvento> items = pedido.getLineas().stream()
                .map(l -> new ItemEvento(l.productoId(), l.cantidad()))
                .toList();

        return new PedidoCancelado(
                UUID.randomUUID().toString(),
                Instant.now(),
                pedido.getId(),
                pedido.getClienteId(),
                motivo,
                huboCobro,
                items);
    }


    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
