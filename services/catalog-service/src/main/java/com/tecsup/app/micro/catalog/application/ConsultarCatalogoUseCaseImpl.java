package com.tecsup.app.micro.catalog.application;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import com.tecsup.app.micro.catalog.domain.model.Restaurante;
import com.tecsup.app.micro.catalog.domain.repository.ProductoRepository;
import com.tecsup.app.micro.catalog.domain.repository.RestauranteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class ConsultarCatalogoUseCaseImpl implements ConsultarCatalogoUseCase {

    private final ProductoRepository productos;
    private final RestauranteRepository restaurantes;

    @Override
    @Transactional(readOnly = true)
    public Producto productoPorId(Long id) {
        return productos.obtener(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> productos() {
        return productos.buscarTodos();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> productosDeRestaurante(Long restauranteId) {
        // Comprueba primero que el restaurante existe: así una petición por un
        // restaurante inexistente devuelve 404 y no una lista vacía, que haría
        // pensar al cliente que existe pero no tiene productos.
        restaurantes.obtener(restauranteId);
        return productos.buscarPorRestaurante(restauranteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Restaurante> restaurantes() {
        return restaurantes.buscarTodos();
    }
}
