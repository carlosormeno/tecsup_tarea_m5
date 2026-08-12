package com.tecsup.app.micro.user.infrastructure.web.dto;

import com.tecsup.app.micro.user.application.AutenticarUsuarioUseCase.Autenticacion;

/**
 * Respuesta del login.
 *
 * `tipo: Bearer` le dice al cliente cómo usar el token:
 * `Authorization: Bearer <token>`.
 */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        Long usuarioId,
        String nombre
) {

    public static LoginResponse de(Autenticacion autenticacion) {
        return new LoginResponse(
                autenticacion.token(),
                "Bearer",
                autenticacion.expiraEnSegundos(),
                autenticacion.usuarioId(),
                autenticacion.nombre());
    }
}
