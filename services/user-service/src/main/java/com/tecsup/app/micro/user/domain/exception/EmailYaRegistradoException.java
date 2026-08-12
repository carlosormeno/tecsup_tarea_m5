package com.tecsup.app.micro.user.domain.exception;

/** El email ya está en uso. Fallo determinista. */
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("Ya existe un usuario con el email " + email);
    }
}
