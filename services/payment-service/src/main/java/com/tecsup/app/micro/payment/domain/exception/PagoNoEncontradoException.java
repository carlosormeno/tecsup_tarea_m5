package com.tecsup.app.micro.payment.domain.exception;

/** Fallo determinista: no reintentable. */
public class PagoNoEncontradoException extends RuntimeException {

    public PagoNoEncontradoException(String referencia) {
        super("No existe pago para " + referencia);
    }
}
