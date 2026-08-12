package com.tecsup.app.micro.payment.domain.model;

import com.tecsup.app.micro.payment.domain.exception.TransicionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas del agregado Pago. Sin Spring, sin base de datos. */
class PagoTest {

    private static final BigDecimal LIMITE = new BigDecimal("500.00");
    private static final UUID PEDIDO = UUID.randomUUID();

    @Test
    @DisplayName("un monto dentro del límite se aprueba y genera referencia")
    void apruebaDentroDelLimite() {
        Pago pago = Pago.procesar(PEDIDO, 1L, new BigDecimal("86.80"), LIMITE);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.fueAprobado()).isTrue();
        assertThat(pago.getReferencia()).startsWith("tx-");
        assertThat(pago.getMotivo()).isNull();
    }

    @Test
    @DisplayName("un monto por encima del límite se rechaza con motivo y sin referencia")
    void rechazaSobreElLimite() {
        Pago pago = Pago.procesar(PEDIDO, 1L, new BigDecimal("750.00"), LIMITE);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(pago.fueAprobado()).isFalse();
        assertThat(pago.getReferencia()).isNull();
        assertThat(pago.getMotivo()).contains("supera el límite");
    }

    @Test
    @DisplayName("el límite exacto se aprueba: rechaza solo lo que lo supera")
    void elLimiteExactoSeAprueba() {
        Pago pago = Pago.procesar(PEDIDO, 1L, new BigDecimal("500.00"), LIMITE);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
    }

    @Test
    @DisplayName("un pago aprobado se puede reembolsar")
    void reembolsaAprobado() {
        Pago pago = Pago.procesar(PEDIDO, 1L, new BigDecimal("86.80"), LIMITE);

        pago.reembolsar("El cliente canceló");

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.REEMBOLSADO);
        assertThat(pago.getMotivo()).isEqualTo("El cliente canceló");
        assertThat(pago.getEstado().esFinal()).isTrue();
    }

    @Test
    @DisplayName("un pago rechazado no se puede reembolsar: nunca se cobró")
    void noReembolsaRechazado() {
        Pago pago = Pago.procesar(PEDIDO, 1L, new BigDecimal("750.00"), LIMITE);

        assertThatThrownBy(() -> pago.reembolsar("da igual"))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("no se reembolsa dos veces")
    void noReembolsaDosVeces() {
        Pago pago = Pago.procesar(PEDIDO, 1L, new BigDecimal("86.80"), LIMITE);
        pago.reembolsar("primera vez");

        assertThatThrownBy(() -> pago.reembolsar("segunda vez"))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    @DisplayName("un monto negativo no es válido")
    void rechazaMontoNegativo() {
        assertThatThrownBy(() -> Pago.procesar(PEDIDO, 1L, new BigDecimal("-1.00"), LIMITE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }
}
