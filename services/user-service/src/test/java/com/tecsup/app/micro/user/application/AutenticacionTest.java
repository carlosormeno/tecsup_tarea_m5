package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.application.RegistrarUsuarioUseCase.ComandoRegistro;
import com.tecsup.app.micro.user.domain.exception.CredencialesInvalidasException;
import com.tecsup.app.micro.user.domain.exception.EmailYaRegistradoException;
import com.tecsup.app.micro.user.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Registro y login: la puerta de entrada a todo el sistema. */
class AutenticacionTest {

    private Fakes.FakeUsuarios usuarios;
    private RegistrarUsuarioUseCaseImpl registrar;
    private AutenticarUsuarioUseCaseImpl autenticar;

    @BeforeEach
    void preparar() {
        usuarios = new Fakes.FakeUsuarios();
        var cifrador = new Fakes.FakeCifrador();
        registrar = new RegistrarUsuarioUseCaseImpl(usuarios, cifrador);
        autenticar = new AutenticarUsuarioUseCaseImpl(usuarios, cifrador, new Fakes.FakeEmisor());
    }

    private Usuario registrarCarlos() {
        return registrar.registrar(new ComandoRegistro(
                "Carlos Ormeño", "carlos@test.com", "password123", "Av. Arequipa 123", Set.of()));
    }

    @Test
    @DisplayName("el registro guarda el hash, nunca la contraseña en claro")
    void registroCifraLaClave() {
        Usuario usuario = registrarCarlos();

        assertThat(usuario.getPasswordHash()).isNotEqualTo("password123");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash:password123");
        assertThat(usuario.getId()).isNotNull();
    }

    @Test
    @DisplayName("no se puede registrar dos veces el mismo email")
    void emailUnico() {
        registrarCarlos();

        assertThatThrownBy(this::registrarCarlos)
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    @Test
    @DisplayName("el email duplicado se detecta aunque cambien mayúsculas o espacios")
    void emailDuplicadoConOtroFormato() {
        registrarCarlos();

        assertThatThrownBy(() -> registrar.registrar(new ComandoRegistro(
                "Otro", "  CARLOS@TEST.COM ", "otraclave", null, Set.of())))
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    @Test
    @DisplayName("con credenciales correctas devuelve el token")
    void loginCorrecto() {
        Usuario usuario = registrarCarlos();

        var autenticacion = autenticar.autenticar("carlos@test.com", "password123");

        assertThat(autenticacion.token()).isEqualTo("token-de-" + usuario.getId());
        assertThat(autenticacion.usuarioId()).isEqualTo(usuario.getId());
        assertThat(autenticacion.expiraEnSegundos()).isEqualTo(3600);
    }

    @Test
    @DisplayName("con la contraseña equivocada falla")
    void passwordIncorrecta() {
        registrarCarlos();

        assertThatThrownBy(() -> autenticar.autenticar("carlos@test.com", "otra-cosa"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("con un email inexistente falla con el MISMO mensaje")
    void emailInexistente() {
        registrarCarlos();

        // Deliberadamente indistinguible del caso anterior: si el mensaje
        // dijera "ese email no existe", confirmaría qué correos están dados de
        // alta y facilitaría un ataque dirigido.
        assertThatThrownBy(() -> autenticar.autenticar("nadie@test.com", "password123"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
    }

    @Test
    @DisplayName("el login acepta el email con otro formato")
    void loginNormalizaEmail() {
        registrarCarlos();

        var autenticacion = autenticar.autenticar("  Carlos@TEST.com  ", "password123");
        assertThat(autenticacion.token()).isNotBlank();
    }
}
