package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.event.PagoConfirmado;
import com.tecsup.app.micro.payment.domain.event.PagoRechazado;
import com.tecsup.app.micro.payment.domain.model.EstadoPago;
import com.tecsup.app.micro.payment.domain.model.Pago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El primer tramo de la saga. */
class ProcesarPagoUseCaseImplTest {

    private static final BigDecimal LIMITE = new BigDecimal("500.00");

    private Fakes.FakeRepositorio repositorio;
    private Fakes.FakePublicador publicador;
    private ProcesarPagoUseCaseImpl procesarPago;
    private UUID pedidoId;

    @BeforeEach
    void preparar() {
        repositorio = new Fakes.FakeRepositorio();
        publicador = new Fakes.FakePublicador();
        procesarPago = new ProcesarPagoUseCaseImpl(repositorio, publicador, LIMITE);
        pedidoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("un cobro aprobado publica pago.confirmado con su referencia")
    void cobroAprobado() {
        Pago pago = procesarPago.procesar(pedidoId, 1L, new BigDecimal("86.80"));

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(publicador.publicados).hasSize(1);

        PagoConfirmado evento = (PagoConfirmado) publicador.publicados.get(0);
        assertThat(evento.pedidoId()).isEqualTo(pedidoId);
        assertThat(evento.referenciaPago()).startsWith("tx-");
        assertThat(evento.monto()).isEqualByComparingTo("86.80");
        // La clave de partición es el pedido, no el pago
        assertThat(evento.idAgregado()).isEqualTo(pedidoId.toString());
    }

    @Test
    @DisplayName("un cobro rechazado publica pago.rechazado con el motivo")
    void cobroRechazado() {
        procesarPago.procesar(pedidoId, 1L, new BigDecimal("750.00"));

        assertThat(publicador.publicados).hasSize(1);
        PagoRechazado evento = (PagoRechazado) publicador.publicados.get(0);
        assertThat(evento.pedidoId()).isEqualTo(pedidoId);
        assertThat(evento.motivo()).contains("supera el límite");
    }

    @Test
    @DisplayName("nunca termina sin publicar: la saga no se puede quedar colgada")
    void siemprePublicaAlgo() {
        procesarPago.procesar(UUID.randomUUID(), 1L, new BigDecimal("10.00"));
        procesarPago.procesar(UUID.randomUUID(), 1L, new BigDecimal("9999.00"));

        assertThat(publicador.publicados).hasSize(2);
    }

    @Test
    @DisplayName("un pedido.pago-solicitado duplicado no cobra dos veces ni publica de nuevo")
    void idempotencia() {
        procesarPago.procesar(pedidoId, 1L, new BigDecimal("86.80"));
        publicador.limpiar();

        // Kafka entrega al menos una vez: el mismo evento puede repetirse
        Pago segundo = procesarPago.procesar(pedidoId, 1L, new BigDecimal("86.80"));

        assertThat(repositorio.buscarTodos()).hasSize(1);
        assertThat(publicador.publicados).isEmpty();
        assertThat(segundo.getPedidoId()).isEqualTo(pedidoId);
    }
}
