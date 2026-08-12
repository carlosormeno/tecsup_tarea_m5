package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.domain.exception.CredencialesInvalidasException;
import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.CifradorDeClaves;
import com.tecsup.app.micro.user.domain.repository.EmisorDeTokens;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: verificar credenciales y emitir el token.
 *
 * Es el punto de entrada a todo el sistema: el token que sale de aquí es el
 * que los otros cuatro servicios van a validar durante la siguiente hora.
 */
@Slf4j
public class AutenticarUsuarioUseCaseImpl implements AutenticarUsuarioUseCase {

    private final UsuarioRepository usuarios;
    private final CifradorDeClaves cifrador;
    private final EmisorDeTokens emisor;

    /**
     * Hash contra el que se compara cuando el email no existe.
     *
     * Se genera con el propio cifrador en lugar de escribirlo a mano: un hash
     * mal formado haría que BCrypt devolviera false SIN calcular nada, y
     * entonces un email inexistente respondería en microsegundos mientras que
     * una contraseña equivocada tardaría ~100 ms. Esa diferencia es
     * exactamente el canal que se quiere cerrar.
     */
    private final String hashDeDescarte;

    public AutenticarUsuarioUseCaseImpl(UsuarioRepository usuarios,
                                        CifradorDeClaves cifrador,
                                        EmisorDeTokens emisor) {
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.emisor = emisor;
        this.hashDeDescarte = cifrador.cifrar("usuario-inexistente-" + UUID.randomUUID());
    }

    @Override
    @Transactional(readOnly = true)
    public Autenticacion autenticar(String email, String password) {
        String normalizado = email == null ? "" : email.toLowerCase().trim();
        Optional<Usuario> encontrado = usuarios.buscarPorEmail(normalizado);

        // Se comprueba la contraseña incluso si el usuario no existe, contra un
        // hash de descarte, para que ambos casos tarden lo mismo. Si no, la
        // diferencia de tiempo revelaría qué emails están registrados.
        String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashDeDescarte);
        boolean claveCorrecta = cifrador.coincide(password == null ? "" : password, hash);

        if (encontrado.isEmpty() || !claveCorrecta || !encontrado.get().isActivo()) {
            log.warn("Intento de login fallido para {}", normalizado);
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = encontrado.get();
        log.info("Usuario {} autenticado", usuario.getEmail());

        return new Autenticacion(
                emisor.emitirPara(usuario),
                emisor.validezEnSegundos(),
                usuario.getId(),
                usuario.getNombre());
    }
}
