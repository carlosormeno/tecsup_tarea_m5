package com.tecsup.app.micro.order.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados del pedido.
 *
 * Las transiciones válidas viven aquí y no en el servicio de aplicación: es
 * una regla de negocio, no de orquestación. Cualquier intento de saltar de un
 * estado a otro que no esté en este mapa es un error de dominio.
 */
public enum EstadoPedido {

    CREADO,
    PAGADO,
    EN_PREPARACION,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO,
    RECHAZADO;

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            CREADO,         EnumSet.of(PAGADO, RECHAZADO, CANCELADO),
            PAGADO,         EnumSet.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION, EnumSet.of(EN_CAMINO, CANCELADO),
            // Una vez que el repartidor salió ya no se cancela: se entrega o falla.
            EN_CAMINO,      EnumSet.of(ENTREGADO, CANCELADO),
            ENTREGADO,      EnumSet.noneOf(EstadoPedido.class),
            CANCELADO,      EnumSet.noneOf(EstadoPedido.class),
            RECHAZADO,      EnumSet.noneOf(EstadoPedido.class)
    );

    public boolean puedeIrA(EstadoPedido destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public boolean esFinal() {
        return TRANSICIONES.get(this).isEmpty();
    }
}
