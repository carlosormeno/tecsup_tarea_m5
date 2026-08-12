package com.tecsup.app.micro.user.application;

/** Puerto de entrada: login. Devuelve el token que usará todo el sistema. */
public interface AutenticarUsuarioUseCase {

    Autenticacion autenticar(String email, String password);

    record Autenticacion(
            String token,
            long expiraEnSegundos,
            Long usuarioId,
            String nombre
    ) {
    }
}
