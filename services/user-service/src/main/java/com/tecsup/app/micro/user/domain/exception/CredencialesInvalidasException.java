package com.tecsup.app.micro.user.domain.exception;

/**
 * Email inexistente o contraseña incorrecta.
 *
 * Es una sola excepción para los dos casos A PROPÓSITO, y el mensaje no
 * distingue cuál falló. Decir "ese email no existe" le confirmaría a un
 * atacante qué correos están registrados, que es medio trabajo hecho para
 * probar contraseñas después.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
