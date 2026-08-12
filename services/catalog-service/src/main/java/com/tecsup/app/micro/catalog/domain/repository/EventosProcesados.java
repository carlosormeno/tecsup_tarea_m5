package com.tecsup.app.micro.catalog.domain.repository;

/**
 * Puerto de salida para la idempotencia.
 *
 * Existe porque Catálogo es el único servicio donde procesar un evento dos
 * veces corrompe datos de forma irreversible: si `pedido.confirmado` llega
 * repetido y se descuenta el stock dos veces, no hay manera de detectarlo
 * mirando el inventario.
 *
 * En Pedidos la idempotencia es gratis (la máquina de estados ignora una
 * transición ya aplicada) y en Pagos casi (basta comprobar si ya existe un
 * pago para ese pedido). Aquí hace falta llevar la cuenta explícita.
 */
public interface EventosProcesados {

    boolean yaProcesado(String eventoId);

    /**
     * Debe ejecutarse en la MISMA transacción que el cambio de stock: o se
     * guardan los dos o ninguno. Si se marcara el evento en una transacción
     * aparte y el descuento fallara después, el reintento lo daría por hecho
     * y el stock quedaría sin ajustar para siempre.
     */
    void marcarProcesado(String eventoId, String topic);
}
