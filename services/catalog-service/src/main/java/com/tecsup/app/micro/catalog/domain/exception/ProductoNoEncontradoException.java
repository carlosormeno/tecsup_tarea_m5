package com.tecsup.app.micro.catalog.domain.exception;

/** Fallo determinista: no reintentable. */
public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long productoId) {
        super("No existe el producto " + productoId);
    }
}
