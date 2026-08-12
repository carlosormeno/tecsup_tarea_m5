package com.tecsup.app.micro.payment.infrastructure.persistence.adapter;

import com.tecsup.app.micro.payment.domain.model.EstadoPago;
import com.tecsup.app.micro.payment.domain.model.Pago;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mapeo de ida y vuelta entre el agregado y la entidad JPA. */
@DataJpaTest
@Import(PagoRepositoryAdapter.class)
class PagoRepositoryAdapterTest {

    private static final BigDecimal LIMITE = new BigDecimal("500.00");

    @Autowired
    private PagoRepositoryAdapter repositorio;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("un pago guardado se recupera idéntico")
    void idaYVuelta() {
        UUID pedidoId = UUID.randomUUID();
        Pago guardado = repositorio.guardar(
                Pago.procesar(pedidoId, 7L, new BigDecimal("86.80"), LIMITE));

        Pago leido = repositorio.buscarPorId(guardado.getId()).orElseThrow();

        assertThat(leido.getPedidoId()).isEqualTo(pedidoId);
        assertThat(leido.getClienteId()).isEqualTo(7L);
        assertThat(leido.getMonto()).isEqualByComparingTo("86.80");
        assertThat(leido.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(leido.getReferencia()).isEqualTo(guardado.getReferencia());
        assertThat(leido.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("se puede buscar por pedido, que es la clave de la idempotencia")
    void buscaPorPedido() {
        UUID pedidoId = UUID.randomUUID();
        repositorio.guardar(Pago.procesar(pedidoId, 1L, new BigDecimal("50.00"), LIMITE));

        assertThat(repositorio.buscarPorPedido(pedidoId)).isPresent();
        assertThat(repositorio.buscarPorPedido(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("la base impide dos pagos para el mismo pedido")
    void unicidadPorPedido() {
        UUID pedidoId = UUID.randomUUID();
        repositorio.guardar(Pago.procesar(pedidoId, 1L, new BigDecimal("50.00"), LIMITE));

        // Última línea de defensa: aunque el filtro de idempotencia del caso de
        // uso fallara, la restricción UNIQUE impide el doble cobro.
        //
        // Hace falta el flush explícito: save() solo deja la entidad en el
        // contexto de persistencia y la restricción no se comprueba hasta que
        // Hibernate manda el INSERT. En producción eso ocurre al confirmar la
        // transacción; en una prueba con @DataJpaTest, que nunca confirma, sin
        // este flush la prueba pasaría sin comprobar nada.
        assertThatThrownBy(() -> {
            repositorio.guardar(Pago.procesar(pedidoId, 1L, new BigDecimal("50.00"), LIMITE));
            entityManager.flush();
        }).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("el reembolso se persiste sobre el mismo pago")
    void persisteReembolso() {
        UUID pedidoId = UUID.randomUUID();
        Pago pago = repositorio.guardar(
                Pago.procesar(pedidoId, 1L, new BigDecimal("50.00"), LIMITE));

        pago.reembolsar("cancelado por el cliente");
        repositorio.guardar(pago);

        Pago leido = repositorio.buscarPorPedido(pedidoId).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(leido.getMotivo()).isEqualTo("cancelado por el cliente");
        assertThat(repositorio.buscarTodos()).hasSize(1);
    }
}
