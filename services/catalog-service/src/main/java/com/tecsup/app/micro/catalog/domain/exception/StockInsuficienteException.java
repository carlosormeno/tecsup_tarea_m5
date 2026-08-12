package com.tecsup.app.micro.catalog.domain.exception;

/**
 * No hay existencias suficientes para descontar.
 *
 * Cuando esto ocurre al procesar `pedido.confirmado`, el pedido YA está pagado:
 * es una anomalía real del negocio, no un error técnico. Reintentar no la
 * arregla —el stock no va a aparecer solo—, así que el evento va a la DLQ para
 * que alguien lo revise.
 *
 * LIMITACIÓN ASUMIDA: un sistema completo publicaría aquí un evento de
 * compensación para que Pedidos cancelara y Pagos reembolsara. No se
 * implementa, y queda declarado en el documento.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(Long productoId, int disponible, int solicitado) {
        super("El producto %d tiene %d unidades y se solicitaron %d"
                .formatted(productoId, disponible, solicitado));
    }
}
