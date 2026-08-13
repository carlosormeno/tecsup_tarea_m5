package com.tecsup.app.micro.order.domain.event;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Arranca la saga. Lo consume Pagos para iniciar el cobro.
 *
 * No se publica al crear el pedido, sino cuando el cliente pulsa «pagar»:
 * crear el pedido y pagarlo son dos decisiones distintas, y el evento lleva el
 * nombre de la segunda porque es la que de verdad ocurre.
 *
 * El total va en el evento y no lo recalcula Pagos: es el importe congelado en
 * el momento del pedido, y ningún otro servicio tiene por qué saber sumar
 * líneas.
 */
public record PagoSolicitado(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        BigDecimal total
) implements EventoDominio {

    public static PagoSolicitado de(Pedido pedido) {
        return new PagoSolicitado(
                UUID.randomUUID().toString(),
                Instant.now(),
                pedido.getId(),
                pedido.getClienteId(),
                pedido.total());
    }

    @Override
    public String idAgregado() {
        return pedidoId.toString();
    }
}
