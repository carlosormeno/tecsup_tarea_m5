package com.tecsup.app.micro.delivery.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados de la entrega.
 *
 * Es el vocabulario que este servicio comunica hacia fuera en el evento
 * `entrega.estado-cambiado`. Pedidos lo traduce a SU propia máquina de
 * estados: ASIGNADA se convierte allí en EN_PREPARACION, COMPLETADA en
 * ENTREGADO. Cada contexto tiene sus propios nombres y eso es correcto.
 */
public enum EstadoEntrega {

    ASIGNADA,
    EN_CAMINO,
    COMPLETADA,
    FALLIDA;

    private static final Map<EstadoEntrega, Set<EstadoEntrega>> TRANSICIONES = Map.of(
            ASIGNADA,   EnumSet.of(EN_CAMINO, FALLIDA),
            EN_CAMINO,  EnumSet.of(COMPLETADA, FALLIDA),
            COMPLETADA, EnumSet.noneOf(EstadoEntrega.class),
            FALLIDA,    EnumSet.noneOf(EstadoEntrega.class)
    );

    public boolean puedeIrA(EstadoEntrega destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public boolean esFinal() {
        return TRANSICIONES.get(this).isEmpty();
    }
}
