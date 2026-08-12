package com.tecsup.app.micro.catalog.infrastructure.messaging.dto;

/** Item tal como viaja dentro de los eventos de Pedidos. */
public record ItemEventoDTO(Long productoId, int cantidad) {
}
