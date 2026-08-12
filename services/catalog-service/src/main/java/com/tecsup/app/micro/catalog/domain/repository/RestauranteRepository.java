package com.tecsup.app.micro.catalog.domain.repository;

import com.tecsup.app.micro.catalog.domain.exception.RestauranteNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.model.Restaurante;

import java.util.List;
import java.util.Optional;

/** Puerto de salida hacia la persistencia de restaurantes. */
public interface RestauranteRepository {

    Optional<Restaurante> buscarPorId(Long id);

    List<Restaurante> buscarTodos();

    default Restaurante obtener(Long id) {
        return buscarPorId(id).orElseThrow(() -> new RestauranteNoEncontradoException(id));
    }
}
