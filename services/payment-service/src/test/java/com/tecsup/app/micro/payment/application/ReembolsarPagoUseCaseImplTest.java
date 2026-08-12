package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.model.EstadoPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * La compensación de la saga.
 *
 * Lo que más se prueba aquí es la tolerancia: los tres casos en que no hay
 * nada que devolver deben resolverse en silencio, no lanzando. Si lanzaran,
 * cada `pedido.cancelado` de un pedido impago acabaría en la DLQ sin motivo.
 */
class ReembolsarPagoUseCaseImplTest {

    private static final BigDecimal LIMITE = new BigDecimal("500.00");

    private Fakes.FakeRepositorio repositorio;
    private ProcesarPagoUseCaseImpl procesarPago;
    private ReembolsarPagoUseCaseImpl reembolsar;
    private UUID pedidoId;

    @BeforeEach
    void preparar() {
        repositorio = new Fakes.FakeRepositorio();
        procesarPago = new ProcesarPagoUseCaseImpl(repositorio, new Fakes.FakePublicador(), LIMITE);
        reembolsar = new ReembolsarPagoUseCaseImpl(repositorio);
        pedidoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("un pago aprobado se reembolsa")
    void reembolsaAprobado() {
        procesarPago.procesar(pedidoId, 1L, new BigDecimal("86.80"));

        reembolsar.reembolsarPorPedido(pedidoId, "El cliente canceló");

        assertThat(repositorio.buscarPorPedido(pedidoId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.REEMBOLSADO);
    }

    @Test
    @DisplayName("si no existe pago, se ignora en silencio")
    void sinPago() {
        assertThatCode(() -> reembolsar.reembolsarPorPedido(UUID.randomUUID(), "cancelado"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("si el pago fue rechazado, no hay nada que devolver")
    void pagoRechazado() {
        procesarPago.procesar(pedidoId, 1L, new BigDecimal("750.00"));

        assertThatCode(() -> reembolsar.reembolsarPorPedido(pedidoId, "cancelado"))
                .doesNotThrowAnyException();

        assertThat(repositorio.buscarPorPedido(pedidoId).orElseThrow().getEstado())
                .isEqualTo(EstadoPago.RECHAZADO);
    }

    @Test
    @DisplayName("un pedido.cancelado duplicado no reembolsa dos veces")
    void idempotencia() {
        procesarPago.procesar(pedidoId, 1L, new BigDecimal("86.80"));
        reembolsar.reembolsarPorPedido(pedidoId, "primera");

        assertThatCode(() -> reembolsar.reembolsarPorPedido(pedidoId, "duplicado"))
                .doesNotThrowAnyException();

        assertThat(repositorio.buscarPorPedido(pedidoId).orElseThrow().getMotivo())
                .isEqualTo("primera");
    }
}
