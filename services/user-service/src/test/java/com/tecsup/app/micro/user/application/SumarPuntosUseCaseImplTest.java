package com.tecsup.app.micro.user.application;

import com.tecsup.app.micro.user.application.RegistrarUsuarioUseCase.ComandoRegistro;
import com.tecsup.app.micro.user.domain.exception.UsuarioNoEncontradoException;
import com.tecsup.app.micro.user.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Puntos de fidelidad: la única razón por la que este servicio consume eventos.
 *
 * Idempotencia con tabla, igual que en Catálogo: sumar dos veces deja el saldo
 * mal sin dejar rastro.
 */
class SumarPuntosUseCaseImplTest {

    private Fakes.FakeUsuarios usuarios;
    private Fakes.FakePedidosPuntuados puntuados;
    private SumarPuntosUseCaseImpl sumarPuntos;
    private Usuario carlos;

    @BeforeEach
    void preparar() {
        usuarios = new Fakes.FakeUsuarios();
        puntuados = new Fakes.FakePedidosPuntuados();
        sumarPuntos = new SumarPuntosUseCaseImpl(usuarios, puntuados);

        carlos = new RegistrarUsuarioUseCaseImpl(usuarios, new Fakes.FakeCifrador())
                .registrar(new ComandoRegistro("Carlos", "carlos@test.com", "password123",
                        null, Set.of()));
    }

    private int puntosDe(Long id) {
        return usuarios.buscarPorId(id).orElseThrow().getPuntosFidelidad();
    }

    @Test
    @DisplayName("un pedido entregado suma puntos")
    void sumaPuntos() {
        sumarPuntos.porPedidoEntregado(UUID.randomUUID(), carlos.getId(), new BigDecimal("71.80"));

        assertThat(puntosDe(carlos.getId())).isEqualTo(71);
        assertThat(puntuados.historial).hasSize(1);
    }

    @Test
    @DisplayName("el MISMO pedido repetido no suma dos veces")
    void idempotencia() {
        UUID pedidoId = UUID.randomUUID();

        sumarPuntos.porPedidoEntregado(pedidoId, carlos.getId(), new BigDecimal("71.80"));
        sumarPuntos.porPedidoEntregado(pedidoId, carlos.getId(), new BigDecimal("71.80"));

        assertThat(puntosDe(carlos.getId())).isEqualTo(71);
        assertThat(puntuados.historial).hasSize(1);
    }

    @Test
    @DisplayName("pedidos distintos acumulan")
    void acumula() {
        sumarPuntos.porPedidoEntregado(UUID.randomUUID(), carlos.getId(), new BigDecimal("71.80"));
        sumarPuntos.porPedidoEntregado(UUID.randomUUID(), carlos.getId(), new BigDecimal("28.20"));

        assertThat(puntosDe(carlos.getId())).isEqualTo(99);
        assertThat(puntuados.historial).hasSize(2);
    }

    @Test
    @DisplayName("si el usuario no existe falla y NO registra el pedido")
    void usuarioInexistente() {
        UUID pedidoId = UUID.randomUUID();

        assertThatThrownBy(() ->
                sumarPuntos.porPedidoEntregado(pedidoId, 999L, new BigDecimal("50.00")))
                .isInstanceOf(UsuarioNoEncontradoException.class);

        // Si se hubiera registrado antes de fallar, un reintento lo daría por
        // hecho y los puntos se perderían para siempre.
        assertThat(puntuados.yaPuntuado(pedidoId)).isFalse();
    }
}
