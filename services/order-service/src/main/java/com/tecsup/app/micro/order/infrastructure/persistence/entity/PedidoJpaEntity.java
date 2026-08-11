package com.tecsup.app.micro.order.infrastructure.persistence.entity;

import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad de persistencia.
 *
 * Deliberadamente separada de {@code Pedido}: la clase de dominio no lleva
 * anotaciones de JPA. Cuesta un mapeador, y a cambio el dominio se puede
 * probar sin base de datos y evolucionar sin arrastrar el esquema.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedido")
public class PedidoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "direccion_entrega", nullable = false)
    private String direccionEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(length = 500)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "pedido_linea",
            joinColumns = @JoinColumn(name = "pedido_id"))
    private List<LineaPedidoEmbeddable> lineas = new ArrayList<>();
}
