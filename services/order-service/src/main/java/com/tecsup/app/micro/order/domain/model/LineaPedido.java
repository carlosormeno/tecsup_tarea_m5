package com.tecsup.app.micro.order.domain.model;

import java.math.BigDecimal;

/**
 * Línea de un pedido.
 *
 * Guarda una COPIA del nombre y del precio que tenía el producto en el momento
 * de pedirlo. Si mañana el restaurante sube el precio, este pedido no cambia:
 * el histórico tiene que reflejar lo que el cliente aceptó pagar, no lo que
 * cuesta hoy en el catálogo.
 */
public record LineaPedido(
        Long productoId,
        String nombreProducto,
        BigDecimal precioUnitario,
        int cantidad
) {

    public LineaPedido {
        if (productoId == null) {
            throw new IllegalArgumentException("La línea necesita un productoId");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo");
        }
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
