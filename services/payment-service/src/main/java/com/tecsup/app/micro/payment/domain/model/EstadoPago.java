package com.tecsup.app.micro.payment.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Estados de un pago.
 *
 * A diferencia del pedido, aquí no hay estado PENDIENTE: la decisión de cobrar
 * se toma en el momento y el pago nace ya aprobado o rechazado. Con una
 * pasarela externa real sí haría falta ese estado intermedio, porque la
 * respuesta llegaría después.
 */
public enum EstadoPago {

    APROBADO,
    RECHAZADO,
    REEMBOLSADO;

    private static final Map<EstadoPago, Set<EstadoPago>> TRANSICIONES = Map.of(
            APROBADO,    EnumSet.of(REEMBOLSADO),
            RECHAZADO,   EnumSet.noneOf(EstadoPago.class),
            REEMBOLSADO, EnumSet.noneOf(EstadoPago.class)
    );

    public boolean puedeIrA(EstadoPago destino) {
        return TRANSICIONES.get(this).contains(destino);
    }

    public boolean esFinal() {
        return TRANSICIONES.get(this).isEmpty();
    }
}
