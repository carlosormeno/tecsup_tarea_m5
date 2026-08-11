package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.model.EstadoEntrega;

import java.util.UUID;

/**
 * Puerto de entrada de la saga. Lo invocan los consumidores de Kafka.
 *
 * Recibe datos planos y no las clases de evento del adaptador: si este puerto
 * conociera `PagoConfirmadoDTO`, el dominio dependería de la infraestructura y
 * el hexágono se rompería.
 */
public interface AvanzarSagaUseCase {

    void pagoConfirmado(UUID pedidoId, String referenciaPago);

    void pagoRechazado(UUID pedidoId, String motivo);

    void entregaCambioEstado(UUID pedidoId, EstadoEntrega estado, String detalle);
}
