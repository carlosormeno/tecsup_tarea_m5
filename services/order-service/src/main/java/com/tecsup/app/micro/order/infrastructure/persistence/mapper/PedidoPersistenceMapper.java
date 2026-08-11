package com.tecsup.app.micro.order.infrastructure.persistence.mapper;

import com.tecsup.app.micro.order.infrastructure.persistence.entity.LineaPedidoEmbeddable;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.PedidoJpaEntity;
import com.tecsup.app.micro.order.domain.model.LineaPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;

import java.util.List;

/**
 * Traduce entre el agregado de dominio y la entidad JPA.
 *
 * Se escribe a mano en lugar de usar MapStruct: son dos métodos y evita un
 * procesador de anotaciones más en la construcción.
 */
public final class PedidoPersistenceMapper {

    private PedidoPersistenceMapper() {
    }

    public static PedidoJpaEntity aEntidad(Pedido pedido) {
        List<LineaPedidoEmbeddable> lineas = pedido.getLineas().stream()
                .map(l -> new LineaPedidoEmbeddable(
                        l.productoId(), l.nombreProducto(), l.precioUnitario(), l.cantidad()))
                .toList();

        return new PedidoJpaEntity(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getDireccionEntrega(),
                pedido.getEstado(),
                pedido.getMotivo(),
                pedido.getCreadoEn(),
                pedido.getActualizadoEn(),
                new java.util.ArrayList<>(lineas));
    }

    public static Pedido aDominio(PedidoJpaEntity entidad) {
        List<LineaPedido> lineas = entidad.getLineas().stream()
                .map(l -> new LineaPedido(
                        l.getProductoId(), l.getNombreProducto(), l.getPrecioUnitario(), l.getCantidad()))
                .toList();

        return Pedido.reconstituir(
                entidad.getId(),
                entidad.getClienteId(),
                entidad.getDireccionEntrega(),
                lineas,
                entidad.getEstado(),
                entidad.getMotivo(),
                entidad.getCreadoEn(),
                entidad.getActualizadoEn());
    }
}
