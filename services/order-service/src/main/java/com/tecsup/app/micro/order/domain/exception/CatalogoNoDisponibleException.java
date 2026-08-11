package com.tecsup.app.micro.order.domain.exception;

/**
 * El servicio de Catálogo no respondió: caído, timeout o error de red.
 *
 * Fallo TRANSITORIO: reintentarlo tiene sentido, porque el catálogo puede
 * volver. Esta es la distinción que sostiene la política de errores — no todo
 * fallo merece un reintento, ni todo fallo merece ir directo a la DLQ.
 */
public class CatalogoNoDisponibleException extends RuntimeException {

    public CatalogoNoDisponibleException(String detalle, Throwable causa) {
        super("El servicio de Catálogo no está disponible: " + detalle, causa);
    }
}
