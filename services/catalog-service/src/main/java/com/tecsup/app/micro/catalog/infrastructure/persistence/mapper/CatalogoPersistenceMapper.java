package com.tecsup.app.micro.catalog.infrastructure.persistence.mapper;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import com.tecsup.app.micro.catalog.domain.model.Restaurante;
import com.tecsup.app.micro.catalog.infrastructure.persistence.entity.ProductoJpaEntity;
import com.tecsup.app.micro.catalog.infrastructure.persistence.entity.RestauranteJpaEntity;

/** Traduce entre los modelos de dominio y las entidades JPA. */
public final class CatalogoPersistenceMapper {

    private CatalogoPersistenceMapper() {
    }

    public static ProductoJpaEntity aEntidad(Producto producto) {
        return new ProductoJpaEntity(
                producto.getId(),
                producto.getRestauranteId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.isActivo());
    }

    public static Producto aDominio(ProductoJpaEntity entidad) {
        return Producto.reconstituir(
                entidad.getId(),
                entidad.getRestauranteId(),
                entidad.getNombre(),
                entidad.getDescripcion(),
                entidad.getPrecio(),
                entidad.getStock(),
                entidad.isActivo());
    }

    public static Restaurante aDominio(RestauranteJpaEntity entidad) {
        return new Restaurante(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getDireccion(),
                entidad.isActivo());
    }
}
