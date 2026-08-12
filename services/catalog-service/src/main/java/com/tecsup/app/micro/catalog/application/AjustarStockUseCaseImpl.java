package com.tecsup.app.micro.catalog.application;

import com.tecsup.app.micro.catalog.domain.model.Producto;
import com.tecsup.app.micro.catalog.domain.repository.EventosProcesados;
import com.tecsup.app.micro.catalog.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: mantener el inventario al ritmo de los pedidos.
 *
 * Aquí está la única idempotencia con tabla de todo el sistema. Kafka entrega
 * al menos una vez, y descontar stock dos veces deja el inventario mal de
 * forma irreversible: no hay manera de saber, mirando el número, si el
 * descuento ya se aplicó.
 *
 * El registro del evento y el ajuste van en la MISMA transacción. Si se
 * marcara el evento aparte y el descuento fallara después, el reintento lo
 * daría por procesado y el stock nunca se ajustaría.
 */
@Slf4j
@RequiredArgsConstructor
public class AjustarStockUseCaseImpl implements AjustarStockUseCase {

    private static final String TOPIC_DESCUENTO = "pedido.confirmado";
    private static final String TOPIC_REPOSICION = "pedido.cancelado";

    private final ProductoRepository productos;
    private final EventosProcesados eventosProcesados;

    @Override
    @Transactional
    public void descontar(String eventoId, List<ItemPedido> items) {
        if (esDuplicado(eventoId, TOPIC_DESCUENTO)) {
            return;
        }

        for (ItemPedido item : items) {
            Producto producto = productos.obtener(item.productoId());
            producto.descontarStock(item.cantidad());
            productos.guardar(producto);

            log.info("Descontadas {} unidades del producto {}; quedan {}",
                    item.cantidad(), item.productoId(), producto.getStock());
        }

        eventosProcesados.marcarProcesado(eventoId, TOPIC_DESCUENTO);
    }

    @Override
    @Transactional
    public void reponer(String eventoId, List<ItemPedido> items) {
        if (esDuplicado(eventoId, TOPIC_REPOSICION)) {
            return;
        }

        for (ItemPedido item : items) {
            Producto producto = productos.obtener(item.productoId());
            producto.reponerStock(item.cantidad());
            productos.guardar(producto);

            log.info("Repuestas {} unidades del producto {}; quedan {}",
                    item.cantidad(), item.productoId(), producto.getStock());
        }

        eventosProcesados.marcarProcesado(eventoId, TOPIC_REPOSICION);
    }

    private boolean esDuplicado(String eventoId, String topic) {
        if (eventosProcesados.yaProcesado(eventoId)) {
            log.info("Evento {} de {} ya estaba procesado; se ignora", eventoId, topic);
            return true;
        }
        return false;
    }
}
