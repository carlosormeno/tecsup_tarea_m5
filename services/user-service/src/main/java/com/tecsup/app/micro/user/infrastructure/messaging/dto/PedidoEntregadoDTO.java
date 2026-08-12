package com.tecsup.app.micro.user.infrastructure.messaging.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cómo lee este servicio el evento `pedido.entregado`.
 *
 * Nótese que Usuarios escucha el hecho a nivel de PEDIDO y no
 * `entrega.estado-cambiado` con estado COMPLETADA. Los estados internos de
 * Entregas no son asunto suyo: solo le importa que el pedido llegó, que es
 * cuando corresponde premiar al cliente.
 */
public record PedidoEntregadoDTO(
        String eventoId,
        Instant ocurridoEn,
        UUID pedidoId,
        Long clienteId,
        BigDecimal total
) {
}
