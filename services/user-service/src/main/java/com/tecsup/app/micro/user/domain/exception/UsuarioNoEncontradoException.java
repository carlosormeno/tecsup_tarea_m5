package com.tecsup.app.micro.user.domain.exception;

/** Fallo determinista: no reintentable. */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException(Long id) {
        super("No existe el usuario " + id);
    }

    public UsuarioNoEncontradoException(String email) {
        super("No existe el usuario con email " + email);
    }
}
