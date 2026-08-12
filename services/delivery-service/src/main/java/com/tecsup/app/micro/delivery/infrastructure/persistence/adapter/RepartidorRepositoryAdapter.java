package com.tecsup.app.micro.delivery.infrastructure.persistence.adapter;

import com.tecsup.app.micro.delivery.domain.model.Repartidor;
import com.tecsup.app.micro.delivery.domain.repository.RepartidorRepository;
import com.tecsup.app.micro.delivery.infrastructure.persistence.mapper.EntregaPersistenceMapper;
import com.tecsup.app.micro.delivery.infrastructure.persistence.repository.JpaRepartidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RepartidorRepositoryAdapter implements RepartidorRepository {

    private final JpaRepartidorRepository jpa;

    @Override
    public List<Repartidor> buscarActivos() {
        return jpa.findByActivoTrueOrderById().stream()
                .map(EntregaPersistenceMapper::aDominio)
                .toList();
    }
}
