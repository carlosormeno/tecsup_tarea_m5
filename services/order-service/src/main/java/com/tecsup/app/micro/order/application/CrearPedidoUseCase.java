package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.util.List;

/** Puerto de entrada: crear un pedido. Lo invoca el adaptador REST. */
public interface CrearPedidoUseCase {

    Pedido crear(ComandoCrearPedido comando);

    /**
     * Comando de entrada. Lleva solo lo que el cliente puede decidir: qué
     * productos y cuántos. El precio NO viene del cliente — se consulta a
     * Catálogo, porque si no cualquiera podría pedir una pizza a un sol.
     */
    record ComandoCrearPedido(
            Long clienteId,
            String direccionEntrega,
            List<ItemSolicitado> items
    ) {
        public record ItemSolicitado(Long productoId, int cantidad) {
        }
    }
}
