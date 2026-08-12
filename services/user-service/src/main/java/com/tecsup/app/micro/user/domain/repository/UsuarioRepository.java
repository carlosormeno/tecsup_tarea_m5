package com.tecsup.app.micro.user.domain.repository;

import com.tecsup.app.micro.user.domain.exception.UsuarioNoEncontradoException;
import com.tecsup.app.micro.user.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

/** Puerto de salida hacia la persistencia de usuarios. */
public interface UsuarioRepository {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existeEmail(String email);

    List<Usuario> buscarTodos();

    default Usuario obtener(Long id) {
        return buscarPorId(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }
}
