package com.tecsup.app.micro.user.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    private Usuario unUsuario() {
        return Usuario.registrar("Carlos Ormeño", "carlos@test.com", "$2a$10$hashfalso",
                "Av. Arequipa 123", Set.of());
    }

    @Test
    @DisplayName("un usuario nuevo es CLIENTE por defecto y empieza sin puntos")
    void registroPorDefecto() {
        Usuario usuario = unUsuario();

        assertThat(usuario.getRoles()).containsExactly(Rol.CLIENTE);
        assertThat(usuario.getPuntosFidelidad()).isZero();
        assertThat(usuario.isActivo()).isTrue();
    }

    @Test
    @DisplayName("el email se normaliza a minúsculas y sin espacios")
    void normalizaEmail() {
        Usuario usuario = Usuario.registrar("Carlos", "  CARLOS@Test.COM  ", "$2a$10$x", null, Set.of());

        // Evita que "Carlos@test.com" y "carlos@test.com" se registren dos veces
        assertThat(usuario.getEmail()).isEqualTo("carlos@test.com");
    }

    @Test
    @DisplayName("se pueden asignar roles explícitos")
    void rolesExplicitos() {
        Usuario admin = Usuario.registrar("Admin", "admin@test.com", "$2a$10$x", null,
                EnumSet.of(Rol.ADMIN, Rol.CLIENTE));

        assertThat(admin.tieneRol(Rol.ADMIN)).isTrue();
        assertThat(admin.tieneRol(Rol.REPARTIDOR)).isFalse();
    }

    @Test
    @DisplayName("un punto por cada sol, redondeando hacia abajo")
    void sumaPuntos() {
        Usuario usuario = unUsuario();

        usuario.sumarPuntosPor(new BigDecimal("71.80"));
        assertThat(usuario.getPuntosFidelidad()).isEqualTo(71);

        usuario.sumarPuntosPor(new BigDecimal("28.20"));
        assertThat(usuario.getPuntosFidelidad()).isEqualTo(99);
    }

    @Test
    @DisplayName("un email sin arroba no es válido")
    void exigeEmailValido() {
        assertThatThrownBy(() ->
                Usuario.registrar("Carlos", "no-es-un-email", "$2a$10$x", null, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("el usuario necesita contraseña")
    void exigePassword() {
        assertThatThrownBy(() ->
                Usuario.registrar("Carlos", "carlos@test.com", "  ", null, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraseña");
    }

    @Test
    @DisplayName("un total negativo no puede dar puntos")
    void rechazaTotalNegativo() {
        Usuario usuario = unUsuario();

        assertThatThrownBy(() -> usuario.sumarPuntosPor(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
