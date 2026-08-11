package com.tecsup.app.micro.order.infrastructure.web.dto;

import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoResponse(
        UUID id,
        Long clienteId,
        String direccionEntrega,
        EstadoPedido estado,
        String motivo,
        BigDecimal total,
        List<LineaResponse> lineas,
        Instant creadoEn,
        Instant actualizadoEn
) {

    public record LineaResponse(
            Long productoId,
            String nombreProducto,
            BigDecimal precioUnitario,
            int cantidad,
            BigDecimal subtotal
    ) {
    }

    public static PedidoResponse de(Pedido pedido) {
        List<LineaResponse> lineas = pedido.getLineas().stream()
                .map(l -> new LineaResponse(
                        l.productoId(), l.nombreProducto(), l.precioUnitario(), l.cantidad(), l.subtotal()))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getDireccionEntrega(),
                pedido.getEstado(),
                pedido.getMotivo(),
                pedido.total(),
                lineas,
                pedido.getCreadoEn(),
                pedido.getActualizadoEn());
    }
}
