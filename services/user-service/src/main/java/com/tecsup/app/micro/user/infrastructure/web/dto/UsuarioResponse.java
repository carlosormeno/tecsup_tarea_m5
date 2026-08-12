package com.tecsup.app.micro.user.infrastructure.web.dto;

import com.tecsup.app.micro.user.domain.model.Rol;
import com.tecsup.app.micro.user.domain.model.Usuario;

import java.time.Instant;
import java.util.Set;

/**
 * Representación del usuario hacia el exterior.
 *
 * NO incluye `passwordHash`. Aunque sea un hash y no la contraseña, exponerlo
 * permitiría atacarlo sin límite de intentos fuera del sistema.
 */
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String direccion,
        Set<Rol> roles,
        int puntosFidelidad,
        boolean activo,
        Instant creadoEn
) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getDireccion(),
                usuario.getRoles(),
                usuario.getPuntosFidelidad(),
                usuario.isActivo(),
                usuario.getCreadoEn());
    }
}
