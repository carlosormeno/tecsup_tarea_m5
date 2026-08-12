package com.tecsup.app.micro.user.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Cuerpo de POST /auth/login. */
public record LoginRequest(

        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
