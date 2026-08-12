package com.tecsup.app.micro.payment.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Cómo lee este servicio el evento `pedido.cancelado`.
 *
 * El evento original de Pedidos lleva además `clienteId` e `items`, pero Pagos
 * no los necesita: para reembolsar le basta el pedido y el motivo. Omitirlos
 * es correcto y deliberado — cada consumidor lee solo lo suyo.
 */
public record PedidoCanceladoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        String motivo,
        boolean huboCobro
) {
}
