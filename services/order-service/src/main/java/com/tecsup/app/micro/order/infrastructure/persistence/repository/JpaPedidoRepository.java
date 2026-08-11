package com.tecsup.app.micro.order.infrastructure.persistence.repository;

import com.tecsup.app.micro.order.infrastructure.persistence.entity.PedidoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositorio de Spring Data. Detalle de infraestructura, no puerto. */
public interface JpaPedidoRepository extends JpaRepository<PedidoJpaEntity, UUID> {

    List<PedidoJpaEntity> findByClienteIdOrderByCreadoEnDesc(Long clienteId);
}
