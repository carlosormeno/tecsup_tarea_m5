package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.util.UUID;

/**
 * Puerto de entrada: el cliente decide pagar un pedido que ya existe.
 *
 * Es el único disparador del cobro en todo el sistema. Pagos no expone ningún
 * endpoint para cobrar: reacciona al evento que publica este caso de uso.
 */
public interface PagarPedidoUseCase {

    Pedido pagar(UUID pedidoId);
}
