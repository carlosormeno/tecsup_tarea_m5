package com.tecsup.app.micro.delivery.infrastructure.persistence.repository;

import com.tecsup.app.micro.delivery.infrastructure.persistence.entity.RepartidorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRepartidorRepository extends JpaRepository<RepartidorJpaEntity, Long> {

    List<RepartidorJpaEntity> findByActivoTrueOrderById();
}
