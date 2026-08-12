package com.tecsup.app.micro.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Pedido que ya sumó puntos.
 *
 * El `pedidoId` es la clave primaria: sirve a la vez de control de
 * idempotencia y de historial de puntos otorgados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedido_puntuado")
public class PedidoPuntuadoJpaEntity {

    @Id
    @Column(name = "pedido_id")
    private UUID pedidoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private int puntos;

    @Column(name = "otorgado_en", nullable = false)
    private Instant otorgadoEn;
}
