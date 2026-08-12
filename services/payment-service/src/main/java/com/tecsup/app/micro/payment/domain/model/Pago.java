package com.tecsup.app.micro.payment.domain.model;

import com.tecsup.app.micro.payment.domain.exception.TransicionInvalidaException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Raíz del agregado Pago.
 *
 * La decisión de aprobar o rechazar vive aquí, en el dominio, y no en una
 * pasarela externa: se acordó no integrar ninguna (ver decisión 11 del
 * seguimiento). El puerto y el adaptador hacia una pasarela real se añadirían
 * sin tocar esta clase, que es justamente lo que promete la arquitectura
 * hexagonal.
 */
public class Pago {

    private final UUID id;
    private final UUID pedidoId;
    private final Long clienteId;
    private final BigDecimal monto;
    private final Instant creadoEn;

    private EstadoPago estado;
    private String referencia;
    private String motivo;
    private Instant actualizadoEn;

    private Pago(UUID id, UUID pedidoId, Long clienteId, BigDecimal monto, EstadoPago estado,
                 String referencia, String motivo, Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.monto = monto;
        this.estado = estado;
        this.referencia = referencia;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    /**
     * Procesa el cobro de un pedido y decide en el acto.
     *
     * La regla es deliberadamente determinista —un límite por importe— y no
     * aleatoria: en la sustentación hay que poder provocar un rechazo cuando
     * se quiera, no cuando toque. Un pedido por encima del límite siempre
     * falla, y eso permite demostrar la compensación de la saga en vivo.
     *
     * @param limiteAutorizado importe máximo que este servicio aprueba;
     *                         se configura desde fuera, la regla vive aquí
     */
    public static Pago procesar(UUID pedidoId, Long clienteId, BigDecimal monto,
                                BigDecimal limiteAutorizado) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("El pago necesita un pedido");
        }
        if (clienteId == null) {
            throw new IllegalArgumentException("El pago necesita un cliente");
        }
        if (monto == null || monto.signum() < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo");
        }

        Instant ahora = Instant.now();
        boolean superaLimite = monto.compareTo(limiteAutorizado) > 0;

        if (superaLimite) {
            return new Pago(UUID.randomUUID(), pedidoId, clienteId, monto,
                    EstadoPago.RECHAZADO, null,
                    "El monto %s supera el límite autorizado de %s".formatted(monto, limiteAutorizado),
                    ahora, ahora);
        }

        return new Pago(UUID.randomUUID(), pedidoId, clienteId, monto,
                EstadoPago.APROBADO, "tx-" + UUID.randomUUID(), null, ahora, ahora);
    }

    /** Reconstruye un pago existente. La usa el adaptador de persistencia. */
    public static Pago reconstituir(UUID id, UUID pedidoId, Long clienteId, BigDecimal monto,
                                    EstadoPago estado, String referencia, String motivo,
                                    Instant creadoEn, Instant actualizadoEn) {
        return new Pago(id, pedidoId, clienteId, monto, estado, referencia, motivo,
                creadoEn, actualizadoEn);
    }

    /**
     * Devuelve el dinero. Solo tiene sentido sobre un pago aprobado: un pago
     * rechazado nunca cobró nada, y uno ya reembolsado no se devuelve dos veces.
     */
    public void reembolsar(String motivoReembolso) {
        if (!estado.puedeIrA(EstadoPago.REEMBOLSADO)) {
            throw new TransicionInvalidaException(id, estado, EstadoPago.REEMBOLSADO);
        }
        this.estado = EstadoPago.REEMBOLSADO;
        this.motivo = motivoReembolso;
        this.actualizadoEn = Instant.now();
    }

    public boolean fueAprobado() {
        return estado == EstadoPago.APROBADO;
    }

    public boolean estaEn(EstadoPago esperado) {
        return this.estado == esperado;
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public Long getClienteId() { return clienteId; }
    public BigDecimal getMonto() { return monto; }
    public EstadoPago getEstado() { return estado; }
    public String getReferencia() { return referencia; }
    public String getMotivo() { return motivo; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
