package com.tecsup.app.micro.delivery.infrastructure.persistence.repository;

import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import com.tecsup.app.micro.delivery.infrastructure.persistence.entity.EntregaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface JpaEntregaRepository extends JpaRepository<EntregaJpaEntity, UUID> {

    Optional<EntregaJpaEntity> findByPedidoId(UUID pedidoId);

    long countByRepartidorIdAndEstadoIn(Long repartidorId, Collection<EstadoEntrega> estados);
}
