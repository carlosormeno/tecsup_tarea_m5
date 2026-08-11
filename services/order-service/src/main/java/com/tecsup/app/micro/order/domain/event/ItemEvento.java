package com.tecsup.app.micro.order.domain.event;

/**
 * Item tal como viaja dentro de un evento.
 *
 * Solo lleva producto y cantidad, que es lo único que Catálogo necesita para
 * ajustar el stock. Ni nombre ni precio: un evento debe cargar lo mínimo que
 * sus consumidores requieren, no una copia completa del agregado.
 */
public record ItemEvento(Long productoId, int cantidad) {
}
