package com.tecsup.app.micro.order.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cómo lee ESTE servicio el evento `pago.confirmado`.
 *
 * Es una clase propia de order-service, no la clase que payment-service usa
 * para publicarlo. Compartir la clase entre servicios los ataría a compilar
 * juntos; con un DTO por cada lado, Pagos puede añadir campos y aquí no se
 * rompe nada.
 */
public record PagoConfirmadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        String referenciaPago,
        BigDecimal monto
) {
}
