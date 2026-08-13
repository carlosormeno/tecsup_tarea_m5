package com.tecsup.app.micro.order.infrastructure.persistence.repository;

import com.tecsup.app.micro.order.infrastructure.persistence.entity.PedidoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Repositorio de Spring Data. Detalle de infraestructura, no puerto. */
public interface JpaPedidoRepository extends JpaRepository<PedidoJpaEntity, UUID> {

    List<PedidoJpaEntity> findByClienteIdOrderByCreadoEnDesc(Long clienteId);

    /**
     * Con orden explícito, y no `findAll()`.
     *
     * Sin ORDER BY el orden lo decide el plan de Postgres y además cambia al
     * actualizar una fila, porque la versión nueva se escribe al final del
     * heap. Las dos listas del sistema —pedidos y entregas— ordenan igual para
     * que se puedan leer una al lado de la otra.
     */
    List<PedidoJpaEntity> findAllByOrderByCreadoEnDesc();
}
