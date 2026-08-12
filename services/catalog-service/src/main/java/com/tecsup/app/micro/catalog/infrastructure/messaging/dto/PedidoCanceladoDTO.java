package com.tecsup.app.micro.catalog.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cómo lee este servicio el evento `pedido.cancelado`.
 *
 * Incluye `huboCobro` porque distingue dos situaciones muy distintas: si hubo
 * cobro, el pedido llegó a confirmarse y el stock se descontó, así que hay que
 * reponerlo. Si no lo hubo, el pedido se rechazó antes de confirmarse y nunca
 * se tocó el inventario.
 */
public record PedidoCanceladoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        String motivo,
        boolean huboCobro,
        List<ItemEventoDTO> items
) {
}
