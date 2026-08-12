package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.exception.EmailYaRegistradoException;
import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.CifradorDeClaves;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class RegistrarUsuarioUseCaseImpl implements RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final CifradorDeClaves cifrador;

    @Override
    @Transactional
    public Usuario registrar(ComandoRegistro comando) {
        String email = comando.email() == null ? "" : comando.email().toLowerCase().trim();

        if (usuarios.existeEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }

        Usuario usuario = usuarios.guardar(Usuario.registrar(
                comando.nombre(),
                email,
                cifrador.cifrar(comando.password()),
                comando.direccion(),
                comando.roles()));

        // Se registra el email, nunca la contraseña ni el hash.
        log.info("Usuario {} registrado con id {}", usuario.getEmail(), usuario.getId());
        return usuario;
    }
}
