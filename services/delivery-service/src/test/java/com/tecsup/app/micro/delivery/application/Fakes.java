package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.event.EventoDominio;
import com.tecsup.app.micro.delivery.domain.event.PublicadorEventos;
import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import com.tecsup.app.micro.delivery.domain.model.Repartidor;
import com.tecsup.app.micro.delivery.domain.repository.EntregaRepository;
import com.tecsup.app.micro.delivery.domain.repository.RepartidorRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Dobles de los puertos de salida. */
final class Fakes {

    private Fakes() {
    }

    static class FakeEntregas implements EntregaRepository {
        private final Map<UUID, Entrega> datos = new HashMap<>();

        @Override
        public Entrega guardar(Entrega entrega) {
            datos.put(entrega.getId(), entrega);
            return entrega;
        }

        @Override
        public Optional<Entrega> buscarPorId(UUID id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Optional<Entrega> buscarPorPedido(UUID pedidoId) {
            return datos.values().stream()
                    .filter(e -> e.getPedidoId().equals(pedidoId)).findFirst();
        }

        @Override
        public List<Entrega> buscarTodas() {
            return List.copyOf(datos.values());
        }

        @Override
        public long contarEnCursoDe(Long repartidorId) {
            return datos.values().stream()
                    .filter(e -> e.getRepartidorId().equals(repartidorId))
                    .filter(e -> e.estaEn(EstadoEntrega.ASIGNADA) || e.estaEn(EstadoEntrega.EN_CAMINO))
                    .count();
        }
    }

    static class FakeRepartidores implements RepartidorRepository {
        private final List<Repartidor> activos = new ArrayList<>();

        FakeRepartidores con(Long... ids) {
            for (Long id : ids) {
                activos.add(new Repartidor(id, "Repartidor " + id, "999", true));
            }
            return this;
        }

        @Override
        public List<Repartidor> buscarActivos() {
            return List.copyOf(activos);
        }
    }

    static class FakePublicador implements PublicadorEventos {
        final List<EventoDominio> publicados = new ArrayList<>();

        @Override
        public void publicar(EventoDominio evento) {
            publicados.add(evento);
        }

        void limpiar() {
            publicados.clear();
        }
    }
}
