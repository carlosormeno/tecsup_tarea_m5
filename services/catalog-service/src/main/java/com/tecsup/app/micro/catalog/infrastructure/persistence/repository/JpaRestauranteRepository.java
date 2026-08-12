package com.tecsup.app.micro.catalog.infrastructure.persistence.repository;

import com.tecsup.app.micro.catalog.infrastructure.persistence.entity.RestauranteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRestauranteRepository extends JpaRepository<RestauranteJpaEntity, Long> {
}
