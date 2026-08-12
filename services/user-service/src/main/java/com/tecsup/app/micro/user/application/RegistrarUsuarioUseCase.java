package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.model.Rol;
import com.tecsup.app.micro.user.domain.model.Usuario;

import java.util.Set;

/** Puerto de entrada: alta de un usuario. */
public interface RegistrarUsuarioUseCase {

    Usuario registrar(ComandoRegistro comando);

    /**
     * La contraseña viaja EN CLARO hasta aquí y se cifra dentro del caso de
     * uso. Es correcto: el cifrado es responsabilidad del servidor, no del
     * adaptador web. Lo que nunca debe salir de aquí en claro es hacia la
     * base de datos ni hacia los logs.
     */
    record ComandoRegistro(
            String nombre,
            String email,
            String password,
            String direccion,
            Set<Rol> roles
    ) {
    }
}
