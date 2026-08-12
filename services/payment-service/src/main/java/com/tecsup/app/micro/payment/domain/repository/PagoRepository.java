package com.tecsup.app.micro.payment.domain.repository;

import com.tecsup.app.micro.payment.domain.model.Pago;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida hacia la persistencia. Habla de Pago, no de entidades JPA. */
public interface PagoRepository {

    Pago guardar(Pago pago);

    Optional<Pago> buscarPorId(UUID id);

    /**
     * Clave de la idempotencia: antes de cobrar se comprueba si ya existe un
     * pago para ese pedido. Si `pedido.creado` llega dos veces, el segundo se
     * ignora en lugar de cobrar por duplicado.
     */
    Optional<Pago> buscarPorPedido(UUID pedidoId);

    List<Pago> buscarTodos();
}
