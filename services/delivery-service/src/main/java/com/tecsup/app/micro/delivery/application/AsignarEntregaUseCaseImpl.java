package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.event.EntregaEstadoCambiado;
import com.tecsup.app.micro.delivery.domain.event.PublicadorEventos;
import com.tecsup.app.micro.delivery.domain.exception.SinRepartidoresException;
import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.Repartidor;
import com.tecsup.app.micro.delivery.domain.repository.EntregaRepository;
import com.tecsup.app.micro.delivery.domain.repository.RepartidorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: asignar repartidor y anunciar que la entrega está en marcha.
 *
 * Ver ADR-007 sobre la doble escritura BD/Kafka: guardar y publicar no
 * comparten transacción, y el riesgo se asume de forma consciente.
 */
@Slf4j
@RequiredArgsConstructor
public class AsignarEntregaUseCaseImpl implements AsignarEntregaUseCase {

    private final EntregaRepository entregas;
    private final RepartidorRepository repartidores;
    private final PublicadorEventos publicador;

    @Override
    @Transactional
    public Entrega asignar(UUID pedidoId, Long clienteId, String direccion) {
        // Idempotencia: Kafka entrega al menos una vez, y crear dos entregas
        // para el mismo pedido ocuparía a dos repartidores con un solo encargo.
        Optional<Entrega> existente = entregas.buscarPorPedido(pedidoId);
        if (existente.isPresent()) {
            log.info("El pedido {} ya tenía entrega ({}); se ignora el evento duplicado",
                    pedidoId, existente.get().getEstado());
            return existente.get();
        }

        Entrega entrega = entregas.guardar(
                Entrega.asignar(pedidoId, clienteId, direccion, elegirRepartidor()));

        log.info("Entrega {} del pedido {} asignada al repartidor {}",
                entrega.getId(), pedidoId, entrega.getRepartidorId());

        publicador.publicar(EntregaEstadoCambiado.de(entrega));
        return entrega;
    }

    /**
     * Reparto de carga sencillo: el repartidor activo con menos entregas en
     * curso. Evita que todos los pedidos caigan sobre el primero de la lista.
     *
     * SIMPLIFICACIÓN ASUMIDA: hace una consulta de conteo por repartidor
     * (N+1). Con una plantilla pequeña es irrelevante; con cientos habría que
     * resolverlo en una sola consulta agregada.
     */
    private Repartidor elegirRepartidor() {
        List<Repartidor> activos = repartidores.buscarActivos();

        if (activos.isEmpty()) {
            // Transitoria a propósito: un repartidor puede activarse en
            // cualquier momento, así que este fallo SÍ se reintenta.
            throw new SinRepartidoresException();
        }

        return activos.stream()
                .min(Comparator.comparingLong(r -> entregas.contarEnCursoDe(r.id())))
                .orElseThrow(SinRepartidoresException::new);
    }
}
