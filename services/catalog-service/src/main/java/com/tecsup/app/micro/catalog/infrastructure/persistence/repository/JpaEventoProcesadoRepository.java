package com.tecsup.app.micro.catalog.infrastructure.persistence.repository;

import com.tecsup.app.micro.catalog.infrastructure.persistence.entity.EventoProcesadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEventoProcesadoRepository extends JpaRepository<EventoProcesadoJpaEntity, String> {
}
