package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class ConsultarUsuariosUseCaseImpl implements ConsultarUsuariosUseCase {

    private final UsuarioRepository usuarios;

    @Override
    @Transactional(readOnly = true)
    public Usuario porId(Long id) {
        return usuarios.obtener(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> todos() {
        return usuarios.buscarTodos();
    }
}
