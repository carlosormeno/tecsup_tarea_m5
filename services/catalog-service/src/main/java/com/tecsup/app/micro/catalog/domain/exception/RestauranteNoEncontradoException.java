package com.tecsup.app.micro.catalog.domain.exception;

/** Fallo determinista: no reintentable. */
public class RestauranteNoEncontradoException extends RuntimeException {

    public RestauranteNoEncontradoException(Long restauranteId) {
        super("No existe el restaurante " + restauranteId);
    }
}
