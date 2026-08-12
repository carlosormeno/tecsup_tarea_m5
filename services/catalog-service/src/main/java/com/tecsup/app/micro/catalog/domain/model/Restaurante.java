package com.tecsup.app.micro.catalog.domain.model;

/** Restaurante que ofrece productos en el catálogo. */
public record Restaurante(
        Long id,
        String nombre,
        String direccion,
        boolean activo
) {

    public Restaurante {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El restaurante necesita un nombre");
        }
    }
}
