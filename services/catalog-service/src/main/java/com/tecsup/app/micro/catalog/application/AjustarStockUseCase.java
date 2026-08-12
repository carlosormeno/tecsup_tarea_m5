package com.tecsup.app.micro.catalog.application;

import java.util.List;

/**
 * Puerto de entrada: ajustes de inventario disparados por eventos de Pedidos.
 *
 * Recibe el `eventoId` porque la idempotencia es responsabilidad de este caso
 * de uso, no del adaptador: es una decisión de negocio saber si un ajuste ya
 * se aplicó.
 */
public interface AjustarStockUseCase {

    /** Al confirmarse un pedido: descuenta lo vendido. */
    void descontar(String eventoId, List<ItemPedido> items);

    /** Al cancelarse un pedido: devuelve al inventario lo que se había descontado. */
    void reponer(String eventoId, List<ItemPedido> items);

    record ItemPedido(Long productoId, int cantidad) {
    }
}
