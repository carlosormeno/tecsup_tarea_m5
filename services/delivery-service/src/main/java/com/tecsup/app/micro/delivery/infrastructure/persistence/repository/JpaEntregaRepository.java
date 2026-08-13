package com.tecsup.app.micro.delivery.infrastructure.persistence.repository;

import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import com.tecsup.app.micro.delivery.infrastructure.persistence.entity.EntregaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaEntregaRepository extends JpaRepository<EntregaJpaEntity, UUID> {

    Optional<EntregaJpaEntity> findByPedidoId(UUID pedidoId);

    /**
     * Con orden explícito, y no `findAll()`.
     *
     * Sin ORDER BY, Postgres devuelve las filas en el orden que le convenga al
     * plan, y ese orden CAMBIA al actualizar una fila: la versión nueva se
     * escribe al final del heap. En una lista que se refresca cada 3 segundos
     * mientras se pulsan botones, eso se ve como filas que saltan de sitio.
     */
    List<EntregaJpaEntity> findAllByOrderByCreadoEnDesc();

    long countByRepartidorIdAndEstadoIn(Long repartidorId, Collection<EstadoEntrega> estados);
}
