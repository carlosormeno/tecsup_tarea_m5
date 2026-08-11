package com.tecsup.app.micro.order.domain.model;

import com.tecsup.app.micro.order.domain.exception.TransicionInvalidaException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Raíz del agregado Pedido.
 *
 * Clase de dominio pura: no conoce JPA, ni Spring, ni Kafka. Toda la regla de
 * negocio del ciclo de vida del pedido vive aquí, y los adaptadores se limitan
 * a traducirla desde y hacia el mundo exterior.
 */
public class Pedido {

    private final UUID id;
    private final Long clienteId;
    private final String direccionEntrega;
    private final List<LineaPedido> lineas;
    private final Instant creadoEn;

    private EstadoPedido estado;
    private String motivo;
    private Instant actualizadoEn;

    private Pedido(UUID id, Long clienteId, String direccionEntrega, List<LineaPedido> lineas,
                   EstadoPedido estado, String motivo, Instant creadoEn, Instant actualizadoEn) {
        this.id = id;
        this.clienteId = clienteId;
        this.direccionEntrega = direccionEntrega;
        this.lineas = List.copyOf(lineas);
        this.estado = estado;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    /** Crea un pedido nuevo. Nace siempre en CREADO, a la espera del pago. */
    public static Pedido crear(Long clienteId, String direccionEntrega, List<LineaPedido> lineas) {
        if (clienteId == null) {
            throw new IllegalArgumentException("El pedido necesita un cliente");
        }
        if (direccionEntrega == null || direccionEntrega.isBlank()) {
            throw new IllegalArgumentException("El pedido necesita una dirección de entrega");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("El pedido necesita al menos una línea");
        }
        Instant ahora = Instant.now();
        return new Pedido(UUID.randomUUID(), clienteId, direccionEntrega, lineas,
                EstadoPedido.CREADO, null, ahora, ahora);
    }

    /** Reconstruye un pedido ya existente. La usa el adaptador de persistencia. */
    public static Pedido reconstituir(UUID id, Long clienteId, String direccionEntrega,
                                      List<LineaPedido> lineas, EstadoPedido estado, String motivo,
                                      Instant creadoEn, Instant actualizadoEn) {
        return new Pedido(id, clienteId, direccionEntrega, lineas, estado, motivo, creadoEn, actualizadoEn);
    }

    /**
     * Aplica una transición de estado.
     *
     * Rechaza los saltos que la máquina de estados no permite. Los eventos
     * repetidos NO se filtran aquí: eso lo decide la capa de aplicación, que es
     * la que sabe distinguir un duplicado inofensivo de un error real.
     */
    public void transicionarA(EstadoPedido destino, String motivo) {
        if (!estado.puedeIrA(destino)) {
            throw new TransicionInvalidaException(id, estado, destino);
        }
        this.estado = destino;
        this.motivo = motivo;
        this.actualizadoEn = Instant.now();
    }

    public BigDecimal total() {
        return lineas.stream()
                .map(LineaPedido::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean estaEn(EstadoPedido esperado) {
        return this.estado == esperado;
    }

    public UUID getId() { return id; }
    public Long getClienteId() { return clienteId; }
    public String getDireccionEntrega() { return direccionEntrega; }
    public List<LineaPedido> getLineas() { return Collections.unmodifiableList(lineas); }
    public EstadoPedido getEstado() { return estado; }
    public String getMotivo() { return motivo; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
