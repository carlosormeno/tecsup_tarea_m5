package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.model.Pago;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: consultas de solo lectura. */
public interface ConsultarPagosUseCase {

    Pago porPedido(UUID pedidoId);

    List<Pago> todos();
}
