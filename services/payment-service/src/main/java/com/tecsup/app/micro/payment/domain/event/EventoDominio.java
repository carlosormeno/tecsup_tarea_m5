package com.tecsup.app.micro.payment.domain.event;

import java.time.Instant;

/**
 * Contrato de todo evento de dominio de este servicio.
 *
 * No dice a qué topic va ni con qué clave se particiona: eso es transporte, y
 * el dominio no debe saber que existe Kafka.
 */
public interface EventoDominio {

    String eventoId();

    Instant ocurridoEn();

    /**
     * Identificador del agregado al que se refiere el evento.
     *
     * Aquí es el id del PEDIDO, no el del pago. Así los eventos de pago caen
     * en la misma partición que los del pedido al que corresponden y Pedidos
     * los consume en orden.
     */
    String idAgregado();
}
