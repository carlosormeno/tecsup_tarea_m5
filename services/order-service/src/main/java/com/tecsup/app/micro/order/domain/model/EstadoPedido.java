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

    /** Hecho pero no pagado. El cliente todavía puede echarse atrás. */
    CREADO,
    /** El cliente pidió pagar y se espera la respuesta de Pagos. */
    PAGO_EN_PROCESO,
    PAGADO,
    EN_PREPARACION,
    EN_CAMINO,
    ENTREGADO,
    CANCELADO,
    RECHAZADO;

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            CREADO,          EnumSet.of(PAGO_EN_PROCESO, CANCELADO),
            // De aquí no se sale solo: hay que esperar a que Pagos responda.
            // Que CREADO no pueda ir directo a PAGADO es lo que impide que
            // alguien se salte el paso del cobro.
            PAGO_EN_PROCESO, EnumSet.of(PAGADO, RECHAZADO, CANCELADO),
            PAGADO,          EnumSet.of(EN_PREPARACION, CANCELADO),
            EN_PREPARACION,  EnumSet.of(EN_CAMINO, CANCELADO),
            // Una vez que el repartidor salió ya no se cancela: se entrega o falla.
            EN_CAMINO,       EnumSet.of(ENTREGADO, CANCELADO),
            ENTREGADO,       EnumSet.noneOf(EstadoPedido.class),
            CANCELADO,       EnumSet.noneOf(EstadoPedido.class),
            RECHAZADO,       EnumSet.noneOf(EstadoPedido.class)
    );

    public boolean puedeIrA(EstadoPedido destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public boolean esFinal() {
        return TRANSICIONES.get(this).isEmpty();
    }

    /**
     * ¿Se llegó a cobrar de verdad?
     *
     * Lo decide el estado y no una bandera aparte, que podría contradecirlo.
     * `PAGO_EN_PROCESO` cuenta como "todavía no": si se cancela justo ahí, el
     * cobro puede estar en vuelo, y por eso el reembolso de Pagos tolera que no
     * exista ningún pago que devolver.
     */
    public boolean implicaCobro() {
        return this == PAGADO || this == EN_PREPARACION
                || this == EN_CAMINO || this == ENTREGADO;
    }
}
