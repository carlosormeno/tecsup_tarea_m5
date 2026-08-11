package com.tecsup.app.micro.order.domain.event;

import java.time.Instant;

/**
 * Contrato de todo evento de dominio de este servicio.
 *
 * No dice a qué topic va ni con qué clave se particiona: eso es transporte, y
 * el dominio no debe saber que existe Kafka. Solo declara qué pasó, cuándo y
 * sobre qué agregado. El adaptador de salida se encarga de traducirlo.
 */
public interface EventoDominio {

    String eventoId();

    Instant ocurridoEn();

    /**
     * Identificador del agregado al que se refiere el evento.
     *
     * El adaptador de Kafka lo usa como clave de partición, con lo que todos
     * los eventos de un mismo pedido acaban en la misma partición y se
     * consumen en orden. Pero esa es una decisión del adaptador: aquí esto
     * significa simplemente "de qué pedido habla este evento".
     */
    String idAgregado();
}
