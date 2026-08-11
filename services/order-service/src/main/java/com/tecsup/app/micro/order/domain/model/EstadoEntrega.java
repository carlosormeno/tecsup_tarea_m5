package com.tecsup.app.micro.order.domain.model;

/**
 * Estados que Entregas comunica a Pedidos.
 *
 * Es el vocabulario del evento `entrega.estado-cambiado`, no el estado interno
 * completo de Entregas: ese servicio puede tener los suyos propios, y solo
 * publica los que a Pedidos le importan.
 */
public enum EstadoEntrega {
    ASIGNADA,
    EN_CAMINO,
    COMPLETADA,
    FALLIDA
}
