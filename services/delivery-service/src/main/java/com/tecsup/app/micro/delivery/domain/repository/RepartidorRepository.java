package com.tecsup.app.micro.delivery.domain.repository;

import com.tecsup.app.micro.delivery.domain.model.Repartidor;

import java.util.List;

/** Puerto de salida hacia la persistencia de repartidores. */
public interface RepartidorRepository {

    List<Repartidor> buscarActivos();
}
