package com.tecsup.app.micro.delivery.infrastructure.persistence.adapter;

import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba del adaptador de persistencia de entregas.
 *
 * Existe por un fallo concreto: el panel del repartidor listaba con `findAll()`
 * y las filas cambiaban de sitio solas mientras se pulsaban botones, porque
 * Postgres reescribe al final del heap la fila que se actualiza.
 */
@DataJpaTest
@Import(EntregaRepositoryAdapter.class)
class EntregaRepositoryAdapterTest {

    @Autowired
    private EntregaRepositoryAdapter repositorio;

    private Entrega unaEntrega(Long repartidorId, Instant creadoEn) {
        return Entrega.reconstituir(UUID.randomUUID(), UUID.randomUUID(), 1L,
                "Av. Arequipa 123", repartidorId, EstadoEntrega.ASIGNADA,
                "Asignada", creadoEn, creadoEn);
    }

    @Test
    @DisplayName("una entrega guardada se recupera idéntica")
    void idaYVuelta() {
        Entrega guardada = repositorio.guardar(
                unaEntrega(7L, Instant.parse("2026-08-12T10:00:00Z")));

        Entrega leida = repositorio.obtener(guardada.getId());

        assertThat(leida.getPedidoId()).isEqualTo(guardada.getPedidoId());
        assertThat(leida.getRepartidorId()).isEqualTo(7L);
        assertThat(leida.getEstado()).isEqualTo(EstadoEntrega.ASIGNADA);
        assertThat(leida.getDireccion()).isEqualTo("Av. Arequipa 123");
    }

    @Test
    @DisplayName("el listado va del más reciente al más antiguo y no se reordena al actualizar")
    void ordenEstable() {
        Instant base = Instant.parse("2026-08-12T10:00:00Z");
        Entrega vieja = repositorio.guardar(unaEntrega(1L, base));
        Entrega media = repositorio.guardar(unaEntrega(2L, base.plusSeconds(60)));
        Entrega nueva = repositorio.guardar(unaEntrega(3L, base.plusSeconds(120)));

        assertThat(repositorio.buscarTodas()).extracting(Entrega::getId)
                .containsExactly(nueva.getId(), media.getId(), vieja.getId());

        // Lo que hace el repartidor al pulsar «Recoger y salir»
        vieja.cambiarEstado(EstadoEntrega.EN_CAMINO, "Pedido recogido");
        repositorio.guardar(vieja);

        // Sigue la última: el orden es por fecha de creación, no por el orden
        // físico de las filas. Esta prueba fija el contrato; el reordenamiento
        // real solo se reproduce contra Postgres.
        assertThat(repositorio.buscarTodas()).extracting(Entrega::getId)
                .containsExactly(nueva.getId(), media.getId(), vieja.getId());
    }

    @Test
    @DisplayName("la carga de un repartidor solo cuenta lo que tiene en curso")
    void cargaEnCurso() {
        Instant base = Instant.parse("2026-08-12T10:00:00Z");
        repositorio.guardar(unaEntrega(1L, base));
        Entrega segunda = repositorio.guardar(unaEntrega(1L, base.plusSeconds(60)));

        assertThat(repositorio.contarEnCursoDe(1L)).isEqualTo(2);

        segunda.cambiarEstado(EstadoEntrega.EN_CAMINO, null);
        segunda.cambiarEstado(EstadoEntrega.COMPLETADA, "Entregado");
        repositorio.guardar(segunda);

        // Una entrega completada ya no ocupa al repartidor
        assertThat(repositorio.contarEnCursoDe(1L)).isEqualTo(1);
    }
}
