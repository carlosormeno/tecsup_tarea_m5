package com.tecsup.app.micro.payment.domain.event;

import com.tecsup.app.micro.payment.domain.model.Pago;

import java.time.Instant;
import java.util.UUID;

/**
 * El cobro no se pudo hacer. Lo consume Pedidos, que pasa el pedido a
 * RECHAZADO y dispara la compensación de la saga.
 *
 * CONTRATO: debe coincidir con `PagoRechazadoDTO` de order-service.
 */
public record PagoRechazado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        String motivo
) implements EventoDominio {

    public static PagoRechazado de(Pago pago) {
        return new PagoRechazado(
                UUID.randomUUID().toString(),
                Instant.now(),
                pago.getPedidoId(),
                pago.getMotivo());
    }

    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
