package com.tecsup.app.micro.order.infrastructure.messaging;

/**
 * Nombres de los topics de Kafka.
 *
 * Vive en infraestructura y no en el dominio: un nombre de topic es una
 * decisión de transporte. Si mañana se cambiara Kafka por otra cosa, este
 * archivo desaparecería y el dominio no se enteraría.
 */
public final class Topics {

    private Topics() {
    }

    // Publica
    public static final String PEDIDO_CREADO = "pedido.creado";
    public static final String PEDIDO_CONFIRMADO = "pedido.confirmado";
    public static final String PEDIDO_ENTREGADO = "pedido.entregado";
    public static final String PEDIDO_CANCELADO = "pedido.cancelado";

    // Consume
    public static final String PAGO_CONFIRMADO = "pago.confirmado";
    public static final String PAGO_RECHAZADO = "pago.rechazado";
    public static final String ENTREGA_ESTADO_CAMBIADO = "entrega.estado-cambiado";

    /**
     * Sufijo de las colas de mensajes fallidos de ESTE servicio.
     *
     * `@RetryableTopic` deriva el nombre de la DLT del topic de origen, así que
     * no existe literalmente una sola DLT por servicio en Kafka. Lo que sí
     * consigue un sufijo propio es que dos servicios que consumen el mismo
     * topic no mezclen sus fallos: los de Pedidos van a
     * `pago.confirmado-pedidos-dlt` y los de Catálogo irían a
     * `pedido.confirmado-catalogo-dlt`.
     *
     * La vista unificada por servicio sí existe, pero del lado de Postgres:
     * todos los fallos caen en la tabla `failed_events` y se consultan juntos
     * en GET /api/admin/dlq.
     */
    public static final String SUFIJO_DLT = "-pedidos-dlt";
}
