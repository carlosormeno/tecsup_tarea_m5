package com.tecsup.app.micro.order.infrastructure.messaging.dto;

import com.tecsup.app.micro.order.domain.model.EstadoEntrega;

import java.time.Instant;
import java.util.UUID;

/**
 * Cómo lee este servicio el evento `entrega.estado-cambiado`.
 *
 * Un único topic con el estado dentro, en vez de cuatro topics (asignada,
 * en camino, completada, fallida): un solo consumidor en lugar de cuatro.
 */
public record EntregaEstadoCambiadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        UUID entregaId,
        EstadoEntrega nuevoEstado,
        String detalle
) {
}
