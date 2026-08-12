package com.tecsup.app.micro.catalog.infrastructure.persistence.adapter;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import com.tecsup.app.micro.catalog.domain.repository.ProductoRepository;
import com.tecsup.app.micro.catalog.infrastructure.persistence.mapper.CatalogoPersistenceMapper;
import com.tecsup.app.micro.catalog.infrastructure.persistence.repository.JpaProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductoRepositoryAdapter implements ProductoRepository {

    private final JpaProductoRepository jpa;

    @Override
    public Producto guardar(Producto producto) {
        return CatalogoPersistenceMapper.aDominio(
                jpa.save(CatalogoPersistenceMapper.aEntidad(producto)));
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return jpa.findById(id).map(CatalogoPersistenceMapper::aDominio);
    }

    @Override
    public List<Producto> buscarPorRestaurante(Long restauranteId) {
        return jpa.findByRestauranteIdOrderByNombre(restauranteId).stream()
                .map(CatalogoPersistenceMapper::aDominio)
                .toList();
    }

    @Override
    public List<Producto> buscarTodos() {
        return jpa.findAll().stream().map(CatalogoPersistenceMapper::aDominio).toList();
    }
}
