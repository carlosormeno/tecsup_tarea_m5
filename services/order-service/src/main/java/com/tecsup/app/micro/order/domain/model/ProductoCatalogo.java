package com.tecsup.app.micro.order.domain.model;

import java.math.BigDecimal;

/**
 * Lo que Catálogo responde sobre un producto.
 *
 * Es un objeto de dominio de ESTE servicio, no una clase compartida con
 * catalog-service: Pedidos solo necesita saber si el producto existe, cómo se
 * llama y cuánto cuesta. Todo lo demás (categoría, restaurante, descripción)
 * es asunto de Catálogo y aquí no se modela.
 */
public record ProductoCatalogo(
        Long id,
        String nombre,
        BigDecimal precio,
        boolean disponible
) {
}
