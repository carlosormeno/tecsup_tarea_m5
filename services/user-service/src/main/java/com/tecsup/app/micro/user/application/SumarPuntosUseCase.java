package com.tecsup.app.micro.user.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de entrada: fidelidad.
 *
 * Lo invoca el consumidor de `pedido.entregado`. Es la única razón por la que
 * este servicio está conectado al broker.
 */
public interface SumarPuntosUseCase {

    void porPedidoEntregado(UUID pedidoId, Long clienteId, BigDecimal total);
}
