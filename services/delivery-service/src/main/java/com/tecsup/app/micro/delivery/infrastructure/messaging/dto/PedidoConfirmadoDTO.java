package com.tecsup.app.micro.delivery.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Cómo lee este servicio el evento `pedido.confirmado`.
 *
 * El evento original lleva además `items`, que aquí se omiten: a Entregas no
 * le importa qué se pidió, solo a dónde hay que llevarlo. Catálogo, en cambio,
 * lee justo los items y no la dirección. Cada consumidor toma lo suyo.
 */
public record PedidoConfirmadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        String direccionEntrega
) {
}
