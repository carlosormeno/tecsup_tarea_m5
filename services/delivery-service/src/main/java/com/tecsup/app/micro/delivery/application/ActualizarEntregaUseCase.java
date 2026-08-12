package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;

import java.util.UUID;

/**
 * Puerto de entrada: el repartidor reporta cómo va la entrega.
 *
 * Este sí es REST: lo llama la aplicación del repartidor, no un evento.
 */
public interface ActualizarEntregaUseCase {

    Entrega cambiarEstado(UUID entregaId, EstadoEntrega nuevoEstado, String detalle);
}
