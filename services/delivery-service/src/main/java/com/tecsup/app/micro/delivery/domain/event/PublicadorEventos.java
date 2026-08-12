package com.tecsup.app.micro.delivery.domain.event;

/** Puerto de salida hacia el broker. */
public interface PublicadorEventos {

    void publicar(EventoDominio evento);
}
