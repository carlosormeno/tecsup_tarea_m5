package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.model.Entrega;

import java.util.List;
import java.util.UUID;

/** Puerto de entrada: consultas de solo lectura. */
public interface ConsultarEntregasUseCase {

    Entrega porPedido(UUID pedidoId);

    Entrega porId(UUID entregaId);

    List<Entrega> todas();
}
