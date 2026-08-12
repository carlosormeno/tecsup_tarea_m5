package com.tecsup.app.micro.catalog.application;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import com.tecsup.app.micro.catalog.domain.model.Restaurante;
import com.tecsup.app.micro.catalog.domain.repository.EventosProcesados;
import com.tecsup.app.micro.catalog.domain.repository.ProductoRepository;
import com.tecsup.app.micro.catalog.domain.repository.RestauranteRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Dobles de los puertos de salida. */
final class Fakes {

    private Fakes() {
    }

    static class FakeProductos implements ProductoRepository {
        private final Map<Long, Producto> datos = new HashMap<>();

        FakeProductos conProducto(Long id, int stock) {
            datos.put(id, Producto.reconstituir(id, 1L, "Producto " + id, null,
                    new BigDecimal("35.90"), stock, true));
            return this;
        }

        @Override
        public Producto guardar(Producto producto) {
            datos.put(producto.getId(), producto);
            return producto;
        }

        @Override
        public Optional<Producto> buscarPorId(Long id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public List<Producto> buscarPorRestaurante(Long restauranteId) {
            return datos.values().stream()
                    .filter(p -> p.getRestauranteId().equals(restauranteId)).toList();
        }

        @Override
        public List<Producto> buscarTodos() {
            return List.copyOf(datos.values());
        }
    }

    static class FakeRestaurantes implements RestauranteRepository {
        private final Map<Long, Restaurante> datos = new HashMap<>();

        FakeRestaurantes conRestaurante(Long id) {
            datos.put(id, new Restaurante(id, "Restaurante " + id, "Una dirección", true));
            return this;
        }

        @Override
        public Optional<Restaurante> buscarPorId(Long id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public List<Restaurante> buscarTodos() {
            return List.copyOf(datos.values());
        }
    }

    static class FakeEventosProcesados implements EventosProcesados {
        private final Set<String> procesados = new HashSet<>();
        final List<String> marcados = new ArrayList<>();

        @Override
        public boolean yaProcesado(String eventoId) {
            return procesados.contains(eventoId);
        }

        @Override
        public void marcarProcesado(String eventoId, String topic) {
            procesados.add(eventoId);
            marcados.add(eventoId);
        }
    }
}
