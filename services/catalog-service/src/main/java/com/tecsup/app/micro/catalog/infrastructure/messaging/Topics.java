package com.tecsup.app.micro.catalog.infrastructure.messaging;

/**
 * Topics de Kafka.
 *
 * Este servicio SOLO CONSUME: no publica ningún evento y por eso no tiene
 * KafkaEventPublisher ni KafkaTopicsConfig. No es un olvido — Catálogo no
 * produce ningún hecho que a otro servicio le interese. Un topic sin
 * consumidor sería código muerto.
 */
public final class Topics {

    private Topics() {
    }

    // Consume
    public static final String PEDIDO_CONFIRMADO = "pedido.confirmado";
    public static final String PEDIDO_CANCELADO = "pedido.cancelado";

    /**
     * Sufijo propio para las colas de fallidos.
     *
     * Es imprescindible aquí: `pedido.confirmado` lo consumen Catálogo y
     * Entregas, y `pedido.cancelado` lo consumen Catálogo y Pagos. Sin un
     * sufijo por servicio, los fallos de dos servicios distintos acabarían
     * mezclados en la misma DLT.
     */
    public static final String SUFIJO_DLT = "-catalogo-dlt";
}
