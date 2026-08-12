package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.event.EventoDominio;
import com.tecsup.app.micro.payment.domain.event.PublicadorEventos;
import com.tecsup.app.micro.payment.domain.model.Pago;
import com.tecsup.app.micro.payment.domain.repository.PagoRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Dobles de los puertos de salida, escritos a mano. */
final class Fakes {

    private Fakes() {
    }

    static class FakeRepositorio implements PagoRepository {
        private final Map<UUID, Pago> datos = new HashMap<>();

        @Override
        public Pago guardar(Pago pago) {
            datos.put(pago.getId(), pago);
            return pago;
        }

        @Override
        public Optional<Pago> buscarPorId(UUID id) {
            return Optional.ofNullable(datos.get(id));
        }

        @Override
        public Optional<Pago> buscarPorPedido(UUID pedidoId) {
            return datos.values().stream().filter(p -> p.getPedidoId().equals(pedidoId)).findFirst();
        }

        @Override
        public List<Pago> buscarTodos() {
            return List.copyOf(datos.values());
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
