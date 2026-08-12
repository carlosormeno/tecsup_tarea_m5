package com.tecsup.app.micro.user.domain.repository;

import java.util.UUID;

/**
 * Puerto de salida para la idempotencia de los puntos de fidelidad.
 *
 * Sumar puntos dos veces por el mismo pedido corrompe el saldo sin dejar
 * rastro, igual que descontar stock dos veces en Catálogo. Llevar la cuenta
 * explícita es la única forma de detectarlo.
 *
 * El registro debe ocurrir en la MISMA transacción que la suma de puntos.
 */
public interface PedidosPuntuados {

    boolean yaPuntuado(UUID pedidoId);

    void registrar(UUID pedidoId, Long usuarioId, int puntos);
}
