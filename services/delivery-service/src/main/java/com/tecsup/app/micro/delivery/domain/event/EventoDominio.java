package com.tecsup.app.micro.delivery.domain.event;

import java.time.Instant;

/**
 * Contrato de todo evento de dominio de este servicio.
 *
 * No conoce topics ni claves de partición: eso es transporte y vive en el
 * adaptador de salida.
 */
public interface EventoDominio {

    String eventoId();

    Instant ocurridoEn();

    /**
     * Id del PEDIDO, no el de la entrega. Así los eventos de entrega caen en
     * la misma partición que el resto de eventos de ese pedido y Pedidos los
     * consume en orden.
     */
    String idAgregado();
}
