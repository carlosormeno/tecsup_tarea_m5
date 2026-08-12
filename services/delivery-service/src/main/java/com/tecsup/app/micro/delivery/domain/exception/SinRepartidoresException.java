package com.tecsup.app.micro.delivery.domain.exception;

/**
 * No hay ningún repartidor activo al que asignar la entrega.
 *
 * A diferencia de las otras excepciones de este servicio, esta es
 * TRANSITORIA: un repartidor puede activarse en cualquier momento, así que el
 * evento SÍ se reintenta. No va en la lista de `exclude` del listener.
 *
 * Si tras los reintentos sigue sin haber nadie, acaba en la DLQ, que es lo
 * correcto: el pedido está pagado y sin repartir, y eso necesita que alguien
 * lo mire.
 */
public class SinRepartidoresException extends RuntimeException {

    public SinRepartidoresException() {
        super("No hay repartidores activos disponibles para asignar la entrega");
    }
}
