package com.tecsup.app.micro.payment.domain.exception;

import com.tecsup.app.micro.payment.domain.model.EstadoPago;

import java.util.UUID;

/**
 * Se intentó un cambio de estado que no permite la máquina.
 *
 * Fallo DETERMINISTA: reintentarlo da el mismo resultado, así que va en la
 * lista de no reintentables de los consumidores y pasa directo a la DLQ.
 */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(UUID pagoId, EstadoPago origen, EstadoPago destino) {
        super("El pago %s no puede pasar de %s a %s".formatted(pagoId, origen, destino));
    }
}
