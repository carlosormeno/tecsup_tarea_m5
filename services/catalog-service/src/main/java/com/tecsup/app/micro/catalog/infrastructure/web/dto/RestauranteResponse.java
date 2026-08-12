package com.tecsup.app.micro.catalog.infrastructure.web.dto;

import com.tecsup.app.micro.catalog.domain.model.Restaurante;

public record RestauranteResponse(
        Long id,
        String nombre,
        String direccion,
        boolean activo
) {

    public static RestauranteResponse de(Restaurante restaurante) {
        return new RestauranteResponse(
                restaurante.id(),
                restaurante.nombre(),
                restaurante.direccion(),
                restaurante.activo());
    }
}
