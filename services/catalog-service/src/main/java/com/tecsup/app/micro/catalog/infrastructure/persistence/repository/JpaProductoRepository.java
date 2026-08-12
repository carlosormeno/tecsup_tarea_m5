package com.tecsup.app.micro.catalog.infrastructure.persistence.repository;

import com.tecsup.app.micro.catalog.infrastructure.persistence.entity.ProductoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaProductoRepository extends JpaRepository<ProductoJpaEntity, Long> {

    List<ProductoJpaEntity> findByRestauranteIdOrderByNombre(Long restauranteId);
}
