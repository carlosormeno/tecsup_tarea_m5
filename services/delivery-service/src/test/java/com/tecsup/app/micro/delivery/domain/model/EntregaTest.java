package com.tecsup.app.micro.delivery.domain.model;

import com.tecsup.app.micro.delivery.domain.exception.TransicionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntregaTest {

    private static final Repartidor LUIS = new Repartidor(1L, "Luis Quispe", "999", true);

    private Entrega unaEntrega() {
        return Entrega.asignar(UUID.randomUUID(), 1L, "Av. Arequipa 123", LUIS);
    }

    @Test
    @DisplayName("nace ASIGNADA y con repartidor: no existe entrega sin repartidor")
    void naceAsignada() {
        Entrega entrega = unaEntrega();

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(entrega.getRepartidorId()).isEqualTo(1L);
        assertThat(entrega.getDetalle()).contains("Luis Quispe");
    }

    @Test
    @DisplayName("el camino feliz llega hasta COMPLETADA")
    void caminoFeliz() {
        Entrega entrega = unaEntrega();

        entrega.cambiarEstado(EstadoEntrega.EN_CAMINO, "en ruta");
        entrega.cambiarEstado(EstadoEntrega.COMPLETADA, "entregado en puerta");

        assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.COMPLETADA);
        assertThat(entrega.getEstado().esFinal()).isTrue();
    }

    @Test
    @DisplayName("no se puede saltar de ASIGNADA a COMPLETADA")
    void noPermiteSaltos() {
        Entrega entrega = unaEntrega();

        assertThatThrownBy(() -> entrega.cambiarEstado(EstadoEntrega.COMPLETADA, null))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("puede fallar desde ASIGNADA o desde EN_CAMINO")
    void puedeFallar() {
        Entrega desdeAsignada = unaEntrega();
        desdeAsignada.cambiarEstado(EstadoEntrega.FALLIDA, "nadie en el domicilio");
        assertThat(desdeAsignada.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);

        Entrega desdeEnCamino = unaEntrega();
        desdeEnCamino.cambiarEstado(EstadoEntrega.EN_CAMINO, null);
        desdeEnCamino.cambiarEstado(EstadoEntrega.FALLIDA, "dirección inexistente");
        assertThat(desdeEnCamino.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);
    }

    @Test
    @DisplayName("una entrega completada ya no admite cambios")
    void estadoFinal() {
        Entrega entrega = unaEntrega();
        entrega.cambiarEstado(EstadoEntrega.EN_CAMINO, null);
        entrega.cambiarEstado(EstadoEntrega.COMPLETADA, null);

        assertThatThrownBy(() -> entrega.cambiarEstado(EstadoEntrega.FALLIDA, "tarde"))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("un repartidor inactivo no puede recibir entregas")
    void exigeRepartidorActivo() {
        Repartidor inactivo = new Repartidor(9L, "Pedro Vargas", "999", false);

        assertThatThrownBy(() ->
                Entrega.asignar(UUID.randomUUID(), 1L, "Av. Arequipa 123", inactivo))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("repartidor activo");
    }

    @Test
    @DisplayName("la entrega necesita dirección")
    void exigeDireccion() {
        assertThatThrownBy(() -> Entrega.asignar(UUID.randomUUID(), 1L, "  ", LUIS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dirección");
    }
}
