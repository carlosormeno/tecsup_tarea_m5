package com.tecsup.app.micro.order.domain.event;


/**
 * Puerto de salida hacia el broker.
 *
 * El dominio publica eventos sin saber que existe Kafka. Cambiar a RabbitMQ o
 * a un bus en memoria para pruebas es cambiar el adaptador, nada más.
 */
public interface PublicadorEventos {

    void publicar(EventoDominio evento);
}
