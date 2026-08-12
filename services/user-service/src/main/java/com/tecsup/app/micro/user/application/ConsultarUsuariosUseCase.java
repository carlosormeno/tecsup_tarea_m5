package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.model.Usuario;

import java.util.List;

/** Puerto de entrada: consultas de solo lectura. */
public interface ConsultarUsuariosUseCase {

    Usuario porId(Long id);

    List<Usuario> todos();
}
