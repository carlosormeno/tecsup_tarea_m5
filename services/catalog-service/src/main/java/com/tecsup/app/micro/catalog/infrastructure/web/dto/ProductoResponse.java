package com.tecsup.app.micro.catalog.infrastructure.web.dto;

import com.tecsup.app.micro.catalog.domain.model.Producto;

import java.math.BigDecimal;

/**
 * Representación del producto hacia el exterior.
 *
 * CONTRATO CON order-service: los campos `id`, `nombre`, `precio` y
 * `disponible` los consume su `CatalogoRestAdapter`. Renombrar cualquiera de
 * ellos rompe la creación de pedidos, y no se detectaría al compilar.
 *
 * `disponible` se calcula, no se guarda: es `activo && stock > 0`.
 */
public record ProductoResponse(
        Long id,
        Long restauranteId,
        String nombre,
        String descripcion,
        BigDecimal precio,
        int stock,
        boolean disponible
) {

    public static ProductoResponse de(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getRestauranteId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.estaDisponible());
    }
}
