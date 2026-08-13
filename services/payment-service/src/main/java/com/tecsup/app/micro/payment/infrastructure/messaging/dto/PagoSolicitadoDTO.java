package com.tecsup.app.micro.payment.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cómo lee ESTE servicio el evento `pedido.pago-solicitado`.
 *
 * Es una clase propia de payment-service, no la de order-service. Compartirla
 * ataría a los dos a compilar juntos; con un DTO por lado, Pedidos puede
 * añadir campos y aquí no se rompe nada (Jackson ignora los desconocidos).
 */
public record PagoSolicitadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        BigDecimal total
) {
}
