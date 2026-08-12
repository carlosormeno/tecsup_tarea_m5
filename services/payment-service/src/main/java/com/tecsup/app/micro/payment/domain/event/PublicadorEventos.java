package com.tecsup.app.micro.payment.domain.event;

/**
 * Puerto de salida hacia el broker.
 *
 * Cada servicio define el suyo en lugar de compartir una clase común: así
 * ninguno obliga a los demás a recompilar. Ver la decisión 10 del seguimiento.
 */
public interface PublicadorEventos {

    void publicar(EventoDominio evento);
}
