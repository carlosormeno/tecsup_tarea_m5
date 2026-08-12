package com.tecsup.app.micro.delivery.infrastructure.web.dto;

import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;

import java.time.Instant;
import java.util.UUID;

public record EntregaResponse(
        UUID id,
        UUID pedidoId,
        Long clienteId,
        String direccion,
        Long repartidorId,
        EstadoEntrega estado,
        String detalle,
        Instant creadoEn,
        Instant actualizadoEn
) {

    public static EntregaResponse de(Entrega entrega) {
        return new EntregaResponse(
                entrega.getId(),
                entrega.getPedidoId(),
                entrega.getClienteId(),
                entrega.getDireccion(),
                entrega.getRepartidorId(),
                entrega.getEstado(),
                entrega.getDetalle(),
                entrega.getCreadoEn(),
                entrega.getActualizadoEn());
    }
}
