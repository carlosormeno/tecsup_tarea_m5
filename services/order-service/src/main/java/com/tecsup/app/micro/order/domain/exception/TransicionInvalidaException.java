package com.tecsup.app.micro.order.domain.exception;

import com.tecsup.app.micro.order.domain.model.EstadoPedido;

import java.util.UUID;

/**
 * Se intentó un salto de estado que la máquina no permite.
 *
 * Es un fallo DETERMINISTA: reintentarlo daría exactamente el mismo resultado.
 * Por eso va en la lista de excepciones no reintentables de los consumidores
 * y pasa directo a la DLQ.
 */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(UUID pedidoId, EstadoPedido origen, EstadoPedido destino) {
        super("El pedido %s no puede pasar de %s a %s".formatted(pedidoId, origen, destino));
    }
}
