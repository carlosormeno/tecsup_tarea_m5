package com.tecsup.app.micro.catalog.infrastructure.persistence.adapter;

import com.tecsup.app.micro.catalog.domain.model.Restaurante;
import com.tecsup.app.micro.catalog.domain.repository.RestauranteRepository;
import com.tecsup.app.micro.catalog.infrastructure.persistence.mapper.CatalogoPersistenceMapper;
import com.tecsup.app.micro.catalog.infrastructure.persistence.repository.JpaRestauranteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RestauranteRepositoryAdapter implements RestauranteRepository {

    private final JpaRestauranteRepository jpa;

    @Override
    public Optional<Restaurante> buscarPorId(Long id) {
        return jpa.findById(id).map(CatalogoPersistenceMapper::aDominio);
    }

    @Override
    public List<Restaurante> buscarTodos() {
        return jpa.findAll().stream().map(CatalogoPersistenceMapper::aDominio).toList();
    }
}
