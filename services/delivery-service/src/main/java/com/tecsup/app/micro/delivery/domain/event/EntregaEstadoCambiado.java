package com.tecsup.app.micro.delivery.domain.event;

import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;

import java.time.Instant;
import java.util.UUID;

/**
 * Único evento que publica este servicio.
 *
 * Es un solo topic con el estado dentro, en lugar de cuatro topics (asignada,
 * en camino, completada, fallida): Pedidos necesita un consumidor en vez de
 * cuatro, y añadir un estado nuevo no obliga a crear un topic.
 *
 * CONTRATO: debe coincidir con `EntregaEstadoCambiadoDTO` de order-service.
 */
public record EntregaEstadoCambiado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        UUID entregaId,
        EstadoEntrega nuevoEstado,
        String detalle
) implements EventoDominio {

    public static EntregaEstadoCambiado de(Entrega entrega) {
        return new EntregaEstadoCambiado(
                UUID.randomUUID().toString(),
                Instant.now(),
                entrega.getPedidoId(),
                entrega.getId(),
                entrega.getEstado(),
                entrega.getDetalle());
    }

    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
