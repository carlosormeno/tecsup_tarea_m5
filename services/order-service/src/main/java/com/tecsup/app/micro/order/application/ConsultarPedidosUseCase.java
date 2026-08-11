package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.model.Pedido;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: consultas de solo lectura. */
public interface ConsultarPedidosUseCase {

    Pedido porId(UUID pedidoId);

    List<Pedido> porCliente(Long clienteId);

    List<Pedido> todos();
}
