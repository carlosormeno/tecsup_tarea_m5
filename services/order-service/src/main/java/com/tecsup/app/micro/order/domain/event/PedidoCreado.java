package com.tecsup.app.micro.order.domain.event;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Arranca la saga. Lo consume Pagos para iniciar el cobro. */
public record PedidoCreado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        BigDecimal total
) implements EventoDominio {

    public static PedidoCreado de(Pedido pedido) {
        return new PedidoCreado(
                UUID.randomUUID().toString(),
                Instant.now(),
                pedido.getId(),
                pedido.getClienteId(),
                pedido.total());
    }


    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
