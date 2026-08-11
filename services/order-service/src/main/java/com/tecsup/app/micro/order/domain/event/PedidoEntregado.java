package com.tecsup.app.micro.order.domain.event;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cierre feliz de la saga. Lo consume Usuarios para sumar puntos de fidelidad.
 *
 * Es un evento distinto de `entrega.estado-cambiado` a propósito: Usuarios no
 * debe escuchar los estados internos de Entregas, sino el hecho de negocio a
 * nivel de pedido. Cada contexto publica en su propio nivel de abstracción.
 */
public record PedidoEntregado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        BigDecimal total
) implements EventoDominio {

    public static PedidoEntregado de(Pedido pedido) {
        return new PedidoEntregado(
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
