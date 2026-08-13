package com.tecsup.app.micro.payment.infrastructure.messaging;

/**
 * Nombres de los topics de Kafka. Infraestructura, no dominio: un nombre de
 * topic es una decisión de transporte.
 */
public final class Topics {

    private Topics() {
    }

    // Publica
    public static final String PAGO_CONFIRMADO = "pago.confirmado";
    public static final String PAGO_RECHAZADO = "pago.rechazado";

    // Consume
    /** Lo publica Pedidos cuando el cliente pulsa «pagar». Es lo que dispara el cobro. */
    public static final String PEDIDO_PAGO_SOLICITADO = "pedido.pago-solicitado";
    public static final String PEDIDO_CANCELADO = "pedido.cancelado";

    /**
     * Sufijo propio de este servicio para las colas de fallidos.
     *
     * `pedido.cancelado` lo consumen Pagos y Catálogo; sin un sufijo distinto
     * por servicio, los fallos de ambos se mezclarían en la misma DLT.
     */
    public static final String SUFIJO_DLT = "-pagos-dlt";
}
