package com.tecsup.app.micro.delivery.domain.model;

/** Persona que lleva los pedidos. */
public record Repartidor(
        Long id,
        String nombre,
        String telefono,
        boolean activo
) {

    public Repartidor {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El repartidor necesita un nombre");
        }
    }
}
