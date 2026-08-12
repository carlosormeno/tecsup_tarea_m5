package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.model.Pago;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de entrada: cobrar un pedido.
 *
 * Lo invoca el consumidor de `pedido.creado`. Recibe datos planos, nunca la
 * clase del evento: si este puerto conociera `PedidoCreadoDTO`, la aplicación
 * dependería de la infraestructura.
 */
public interface ProcesarPagoUseCase {

    Pago procesar(UUID pedidoId, Long clienteId, BigDecimal monto);
}
