package com.tecsup.app.micro.delivery.infrastructure.messaging;

/** Topics de Kafka. Infraestructura, no dominio. */
public final class Topics {

    private Topics() {
    }

    // Publica
    public static final String ENTREGA_ESTADO_CAMBIADO = "entrega.estado-cambiado";

    // Consume
    public static final String PEDIDO_CONFIRMADO = "pedido.confirmado";

    /**
     * Sufijo propio para las colas de fallidos.
     *
     * Imprescindible: `pedido.confirmado` lo consumen Entregas y Catálogo.
     * Sin sufijo por servicio, los fallos de ambos se mezclarían.
     */
    public static final String SUFIJO_DLT = "-entregas-dlt";
}
