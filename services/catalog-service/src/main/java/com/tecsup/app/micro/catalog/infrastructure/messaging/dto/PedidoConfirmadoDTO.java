package com.tecsup.app.micro.catalog.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cómo lee este servicio el evento `pedido.confirmado`.
 *
 * El evento original lleva además `clienteId` y `direccionEntrega`, que aquí
 * se omiten: a Catálogo solo le importan los items para ajustar el stock.
 * Cada consumidor lee lo suyo y Jackson descarta el resto.
 */
public record PedidoConfirmadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        List<ItemEventoDTO> items
) {
}
