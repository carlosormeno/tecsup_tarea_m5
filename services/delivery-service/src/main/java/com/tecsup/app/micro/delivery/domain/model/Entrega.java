package com.tecsup.app.micro.delivery.domain.model;

import com.tecsup.app.micro.delivery.domain.exception.TransicionInvalidaException;

import java.time.Instant;
import java.util.UUID;

/**
 * Raíz del agregado Entrega.
 *
 * Nace ya asignada a un repartidor: no existe un estado PENDIENTE porque el
 * único momento en que se crea una entrega es al confirmarse un pedido, y en
 * ese mismo instante se le busca repartidor. Una entrega sin repartidor no
 * tendría sentido.
 */
public class Entrega {

    private final UUID id;
    private final UUID pedidoId;
    private final Long clienteId;
    private final String direccion;
    private final Long repartidorId;
    private final Instant creadoEn;

    private EstadoEntrega estado;
    private String detalle;
    private Instant actualizadoEn;

    private Entrega(UUID id, UUID pedidoId, Long clienteId, String direccion, Long repartidorId,
                    EstadoEntrega estado, String detalle, Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.clienteId = clienteId;
        this.direccion = direccion;
        this.repartidorId = repartidorId;
        this.estado = estado;
        this.detalle = detalle;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public static Entrega asignar(UUID pedidoId, Long clienteId, String direccion,
                                  Repartidor repartidor) {
        if (pedidoId == null) {
            throw new IllegalArgumentException("La entrega necesita un pedido");
        }
        if (direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("La entrega necesita una dirección");
        }
        if (repartidor == null || !repartidor.activo()) {
            throw new IllegalArgumentException("La entrega necesita un repartidor activo");
        }

        Instant ahora = Instant.now();
        return new Entrega(UUID.randomUUID(), pedidoId, clienteId, direccion, repartidor.id(),
                EstadoEntrega.ASIGNADA, "Asignada a " + repartidor.nombre(), ahora, ahora);
    }

    public static Entrega reconstituir(UUID id, UUID pedidoId, Long clienteId, String direccion,
                                       Long repartidorId, EstadoEntrega estado, String detalle,
                                       Instant creadoEn, Instant actualizadoEn) {
        return new Entrega(id, pedidoId, clienteId, direccion, repartidorId, estado, detalle,
                creadoEn, actualizadoEn);
    }

    /** Avanza el estado. Rechaza los saltos que la máquina no permite. */
    public void cambiarEstado(EstadoEntrega destino, String motivo) {
        if (!estado.puedeIrA(destino)) {
            throw new TransicionInvalidaException(id, estado, destino);
        }
        this.estado = destino;
        this.detalle = motivo;
        this.actualizadoEn = Instant.now();
    }

    public boolean estaEn(EstadoEntrega esperado) {
        return this.estado == esperado;
    }

    public UUID getId() { return id; }
    public UUID getPedidoId() { return pedidoId; }
    public Long getClienteId() { return clienteId; }
    public String getDireccion() { return direccion; }
    public Long getRepartidorId() { return repartidorId; }
    public EstadoEntrega getEstado() { return estado; }
    public String getDetalle() { return detalle; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
