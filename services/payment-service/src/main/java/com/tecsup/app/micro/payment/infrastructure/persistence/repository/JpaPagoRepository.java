package com.tecsup.app.micro.payment.infrastructure.persistence.repository;

import com.tecsup.app.micro.payment.infrastructure.persistence.entity.PagoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Repositorio de Spring Data. Detalle de infraestructura, no puerto. */
public interface JpaPagoRepository extends JpaRepository<PagoJpaEntity, UUID> {

    Optional<PagoJpaEntity> findByPedidoId(UUID pedidoId);
}
