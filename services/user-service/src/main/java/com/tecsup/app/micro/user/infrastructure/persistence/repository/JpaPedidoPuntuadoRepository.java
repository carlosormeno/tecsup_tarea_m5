package com.tecsup.app.micro.user.infrastructure.persistence.repository;

import com.tecsup.app.micro.user.infrastructure.persistence.entity.PedidoPuntuadoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPedidoPuntuadoRepository extends JpaRepository<PedidoPuntuadoJpaEntity, UUID> {
}
