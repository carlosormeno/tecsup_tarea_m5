package com.tecsup.app.micro.catalog.domain.repository;

import com.tecsup.app.micro.catalog.domain.exception.ProductoNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.model.Producto;

import java.util.List;
import java.util.Optional;

/** Puerto de salida hacia la persistencia de productos. */
public interface ProductoRepository {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(Long id);

    List<Producto> buscarPorRestaurante(Long restauranteId);

    List<Producto> buscarTodos();

    default Producto obtener(Long id) {
        return buscarPorId(id).orElseThrow(() -> new ProductoNoEncontradoException(id));
    }
}
