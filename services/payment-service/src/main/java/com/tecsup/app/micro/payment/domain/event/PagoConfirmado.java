package com.tecsup.app.micro.payment.domain.event;

import com.tecsup.app.micro.payment.domain.model.Pago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * El cobro salió bien. Lo consume Pedidos para pasar el pedido a PAGADO.
 *
 * CONTRATO: los nombres de los campos deben coincidir con los de
 * `PagoConfirmadoDTO` de order-service. Si aquí se renombra `referenciaPago`,
 * Pedidos recibirá null y no se enterará hasta ejecución. Es el riesgo que
 * asumimos al descartar las pruebas de contrato.
 */
public record PagoConfirmado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        String referenciaPago,
        BigDecimal monto
) implements EventoDominio {

    public static PagoConfirmado de(Pago pago) {
        return new PagoConfirmado(
                UUID.randomUUID().toString(),
                Instant.now(),
                pago.getPedidoId(),
                pago.getReferencia(),
                pago.getMonto());
    }

    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
