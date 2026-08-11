package com.tecsup.app.micro.order.infrastructure.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/** Cómo lee este servicio el evento `pago.rechazado`. */
public record PagoRechazadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        String motivo
) {
}
