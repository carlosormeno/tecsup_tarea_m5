package com.tecsup.app.micro.catalog.application;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import com.tecsup.app.micro.catalog.domain.model.Restaurante;

import java.util.List;

/**
 * Puerto de entrada: consultas del catálogo.
 *
 * `productoPorId` es el que consume order-service por REST antes de crear un
 * pedido. Es la única dependencia síncrona de todo el sistema.
 */
public interface ConsultarCatalogoUseCase {

    Producto productoPorId(Long id);

    List<Producto> productos();

    List<Producto> productosDeRestaurante(Long restauranteId);

    List<Restaurante> restaurantes();
}
