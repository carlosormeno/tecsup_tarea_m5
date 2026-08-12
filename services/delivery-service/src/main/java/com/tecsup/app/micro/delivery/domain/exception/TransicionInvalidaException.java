package com.tecsup.app.micro.delivery.domain.exception;

import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;

import java.util.UUID;

/** Fallo determinista: reintentarlo daría el mismo resultado. */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(UUID entregaId, EstadoEntrega origen, EstadoEntrega destino) {
        super("La entrega %s no puede pasar de %s a %s".formatted(entregaId, origen, destino));
    }
}
