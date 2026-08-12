package com.tecsup.app.micro.delivery.infrastructure.persistence.mapper;

import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.Repartidor;
import com.tecsup.app.micro.delivery.infrastructure.persistence.entity.EntregaJpaEntity;
import com.tecsup.app.micro.delivery.infrastructure.persistence.entity.RepartidorJpaEntity;

/** Traduce entre los modelos de dominio y las entidades JPA. */
public final class EntregaPersistenceMapper {

    private EntregaPersistenceMapper() {
    }

    public static EntregaJpaEntity aEntidad(Entrega entrega) {
        return new EntregaJpaEntity(
                entrega.getId(),
                entrega.getPedidoId(),
                entrega.getClienteId(),
                entrega.getDireccion(),
                entrega.getRepartidorId(),
                entrega.getEstado(),
                entrega.getDetalle(),
                entrega.getCreadoEn(),
                entrega.getActualizadoEn());
    }

    public static Entrega aDominio(EntregaJpaEntity entidad) {
        return Entrega.reconstituir(
                entidad.getId(),
                entidad.getPedidoId(),
                entidad.getClienteId(),
                entidad.getDireccion(),
                entidad.getRepartidorId(),
                entidad.getEstado(),
                entidad.getDetalle(),
                entidad.getCreadoEn(),
                entidad.getActualizadoEn());
    }

    public static Repartidor aDominio(RepartidorJpaEntity entidad) {
        return new Repartidor(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getTelefono(),
                entidad.isActivo());
    }
}
