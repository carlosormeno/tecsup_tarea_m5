package com.tecsup.app.micro.order.domain.exception;

import java.util.UUID;

/** Fallo determinista: no reintentable. */
public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(UUID pedidoId) {
        super("No existe el pedido " + pedidoId);
    }
}
