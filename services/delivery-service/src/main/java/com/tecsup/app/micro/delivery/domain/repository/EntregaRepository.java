package com.tecsup.app.micro.delivery.domain.repository;

import com.tecsup.app.micro.delivery.domain.exception.EntregaNoEncontradaException;
import com.tecsup.app.micro.delivery.domain.model.Entrega;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida hacia la persistencia de entregas. */
public interface EntregaRepository {

    Entrega guardar(Entrega entrega);

    Optional<Entrega> buscarPorId(UUID id);

    /** Clave de la idempotencia: ¿este pedido ya tiene entrega? */
    Optional<Entrega> buscarPorPedido(UUID pedidoId);

    /** Todas las entregas, **de la más reciente a la más antigua**. */
    List<Entrega> buscarTodas();

    /** Entregas ASIGNADA o EN_CAMINO de un repartidor: su carga actual. */
    long contarEnCursoDe(Long repartidorId);

    default Entrega obtener(UUID id) {
        return buscarPorId(id).orElseThrow(
                () -> new EntregaNoEncontradaException("la entrega " + id));
    }
}
