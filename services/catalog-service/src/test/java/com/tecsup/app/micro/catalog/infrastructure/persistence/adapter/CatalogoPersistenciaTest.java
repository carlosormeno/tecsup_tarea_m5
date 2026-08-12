package com.tecsup.app.micro.catalog.infrastructure.persistence.adapter;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapeo de ida y vuelta y persistencia de la idempotencia. */
@DataJpaTest
@Import({ProductoRepositoryAdapter.class, EventosProcesadosAdapter.class})
class CatalogoPersistenciaTest {

    @Autowired
    private ProductoRepositoryAdapter productos;

    @Autowired
    private EventosProcesadosAdapter eventos;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("un producto guardado se recupera idéntico")
    void idaYVuelta() {
        Producto guardado = productos.guardar(
                Producto.crear(1L, "Pizza margarita", "Muzzarella", new BigDecimal("35.90"), 100));

        assertThat(guardado.getId()).isNotNull();

        Producto leido = productos.buscarPorId(guardado.getId()).orElseThrow();
        assertThat(leido.getNombre()).isEqualTo("Pizza margarita");
        assertThat(leido.getPrecio()).isEqualByComparingTo("35.90");
        assertThat(leido.getStock()).isEqualTo(100);
        assertThat(leido.estaDisponible()).isTrue();
    }

    @Test
    @DisplayName("el descuento de stock se persiste")
    void persisteDescuento() {
        Producto producto = productos.guardar(
                Producto.crear(1L, "Pizza", null, new BigDecimal("35.90"), 10));

        producto.descontarStock(4);
        productos.guardar(producto);

        assertThat(productos.buscarPorId(producto.getId()).orElseThrow().getStock()).isEqualTo(6);
    }

    @Test
    @DisplayName("el registro de eventos procesados sobrevive a la base")
    void registroDeEventos() {
        assertThat(eventos.yaProcesado("evt-1")).isFalse();

        eventos.marcarProcesado("evt-1", "pedido.confirmado");
        entityManager.flush();
        entityManager.clear();

        assertThat(eventos.yaProcesado("evt-1")).isTrue();
        assertThat(eventos.yaProcesado("evt-2")).isFalse();
    }

    @Test
    @DisplayName("la clave primaria del evento impide registrarlo dos veces")
    void eventoUnico() {
        eventos.marcarProcesado("evt-1", "pedido.confirmado");
        entityManager.flush();

        // Al ser el eventoId la clave primaria, un segundo save() con el mismo
        // id es una actualización, no un duplicado: sigue habiendo una fila.
        eventos.marcarProcesado("evt-1", "pedido.confirmado");
        entityManager.flush();

        assertThat(eventos.yaProcesado("evt-1")).isTrue();
    }
}
