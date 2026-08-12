package com.tecsup.app.micro.payment.application;

import java.util.UUID;

/** Puerto de entrada: devolver el dinero de un pedido cancelado. */
public interface ReembolsarPagoUseCase {

    void reembolsarPorPedido(UUID pedidoId, String motivo);
}
