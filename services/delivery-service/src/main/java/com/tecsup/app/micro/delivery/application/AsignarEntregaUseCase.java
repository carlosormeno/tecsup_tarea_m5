package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.model.Entrega;

import java.util.UUID;

/**
 * Puerto de entrada: crear la entrega de un pedido confirmado.
 *
 * Lo invoca el consumidor de `pedido.confirmado`. No hay endpoint REST para
 * esto: una entrega solo nace de un pedido confirmado.
 */
public interface AsignarEntregaUseCase {

    Entrega asignar(UUID pedidoId, Long clienteId, String direccion);
}
