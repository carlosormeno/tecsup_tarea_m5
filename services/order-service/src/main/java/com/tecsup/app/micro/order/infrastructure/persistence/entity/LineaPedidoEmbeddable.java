package com.tecsup.app.micro.order.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Línea de pedido tal como se guarda. Espejo de LineaPedido, sin lógica. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class LineaPedidoEmbeddable {

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "nombre_producto", nullable = false)
    private String nombreProducto;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;
}
