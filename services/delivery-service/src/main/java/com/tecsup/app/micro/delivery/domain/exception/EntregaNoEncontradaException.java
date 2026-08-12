package com.tecsup.app.micro.delivery.domain.exception;

/** Fallo determinista: no reintentable. */
public class EntregaNoEncontradaException extends RuntimeException {

    public EntregaNoEncontradaException(String referencia) {
        super("No existe entrega para " + referencia);
    }
}
