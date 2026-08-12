package com.tecsup.app.micro.user.infrastructure.messaging;

/**
 * Topics de Kafka.
 *
 * Este servicio SOLO CONSUME, igual que Catálogo: no tiene nada que anunciar
 * que a otro servicio le importe. Por eso no hay KafkaEventPublisher ni
 * KafkaTopicsConfig.
 */
public final class Topics {

    private Topics() {
    }

    // Consume
    public static final String PEDIDO_ENTREGADO = "pedido.entregado";

    public static final String SUFIJO_DLT = "-usuarios-dlt";
}
