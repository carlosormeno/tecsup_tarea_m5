package com.tecsup.app.micro.payment.infrastructure.persistence.mapper;

import com.tecsup.app.micro.payment.domain.model.Pago;
import com.tecsup.app.micro.payment.infrastructure.persistence.entity.PagoJpaEntity;

/** Traduce entre el agregado de dominio y la entidad JPA. */
public final class PagoPersistenceMapper {

    private PagoPersistenceMapper() {
    }

    public static PagoJpaEntity aEntidad(Pago pago) {
        return new PagoJpaEntity(
                pago.getId(),
                pago.getPedidoId(),
                pago.getClienteId(),
                pago.getMonto(),
                pago.getEstado(),
                pago.getReferencia(),
                pago.getMotivo(),
                pago.getCreadoEn(),
                pago.getActualizadoEn());
    }

    public static Pago aDominio(PagoJpaEntity entidad) {
        return Pago.reconstituir(
                entidad.getId(),
                entidad.getPedidoId(),
                entidad.getClienteId(),
                entidad.getMonto(),
                entidad.getEstado(),
                entidad.getReferencia(),
                entidad.getMotivo(),
                entidad.getCreadoEn(),
                entidad.getActualizadoEn());
    }
}
