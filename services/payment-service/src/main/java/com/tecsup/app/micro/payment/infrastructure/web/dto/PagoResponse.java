package com.tecsup.app.micro.payment.infrastructure.web.dto;

import com.tecsup.app.micro.payment.domain.model.EstadoPago;
import com.tecsup.app.micro.payment.domain.model.Pago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Representación del pago hacia el exterior. */
public record PagoResponse(
        UUID id,
        UUID pedidoId,
        Long clienteId,
        BigDecimal monto,
        EstadoPago estado,
        String referencia,
        String motivo,
        Instant creadoEn,
        Instant actualizadoEn
) {

    public static PagoResponse de(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getPedidoId(),
                pago.getClienteId(),
                pago.getMonto(),
                pago.getEstado(),
                pago.getReferencia(),
                pago.getMotivo(),
                pago.getCreadoEn(),
                pago.getActualizadoEn());
    }
}
