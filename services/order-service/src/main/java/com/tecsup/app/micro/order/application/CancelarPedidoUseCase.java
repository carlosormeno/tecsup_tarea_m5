package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.util.UUID;

/** Puerto de entrada: cancelación pedida por el cliente. */
public interface CancelarPedidoUseCase {

    Pedido cancelar(UUID pedidoId, String motivo);
}
