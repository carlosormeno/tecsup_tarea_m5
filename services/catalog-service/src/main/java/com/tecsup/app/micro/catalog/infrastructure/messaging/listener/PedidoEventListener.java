package com.tecsup.app.micro.catalog.infrastructure.messaging.listener;

import com.tecsup.app.micro.catalog.application.AjustarStockUseCase;
import com.tecsup.app.micro.catalog.domain.exception.ProductoNoEncontradoException;
import com.tecsup.app.micro.catalog.domain.exception.StockInsuficienteException;
import com.tecsup.app.micro.catalog.infrastructure.messaging.Topics;
import com.tecsup.app.micro.catalog.infrastructure.messaging.dto.ItemEventoDTO;
import com.tecsup.app.micro.catalog.infrastructure.messaging.dto.PedidoCanceladoDTO;
import com.tecsup.app.micro.catalog.infrastructure.messaging.dto.PedidoConfirmadoDTO;
import com.tecsup.app.micro.shared.dlq.DeadLetterQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptador de entrada: eventos de Pedidos que afectan al inventario.
 *
 * `StockInsuficienteException` va en `exclude` a propósito: si el stock no
 * alcanza, reintentar tres veces no lo va a hacer aparecer. El evento pasa
 * directo a la DLQ, donde queda como aviso de una anomalía real —un pedido
 * pagado que no se puede servir— que necesita intervención humana.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventListener {

    private final AjustarStockUseCase ajustarStock;
    private final DeadLetterQueue dlq;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            // false: los topics de reintento y DLT los declara KafkaTopicsConfig
            // con 3 particiones. Si los creara la anotación, los haría con una
            // sola y la publicación a la DLT fallaría desde las particiones 1 y 2.
            autoCreateTopics = "false",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    ProductoNoEncontradoException.class,
                    StockInsuficienteException.class,
                    IllegalArgumentException.class
            })
    @KafkaListener(topics = Topics.PEDIDO_CONFIRMADO, groupId = "catalogo-group")
    public void alConfirmarsePedido(PedidoConfirmadoDTO evento) {
        log.info("Recibido pedido.confirmado para el pedido {}: descontando stock de {} items",
                evento.pedidoId(), evento.items().size());

        ajustarStock.descontar(evento.eventoId(), aItems(evento.items()));
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            // false: los topics de reintento y DLT los declara KafkaTopicsConfig
            // con 3 particiones. Si los creara la anotación, los haría con una
            // sola y la publicación a la DLT fallaría desde las particiones 1 y 2.
            autoCreateTopics = "false",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    ProductoNoEncontradoException.class,
                    StockInsuficienteException.class,
                    IllegalArgumentException.class
            })
    @KafkaListener(topics = Topics.PEDIDO_CANCELADO, groupId = "catalogo-group")
    public void alCancelarsePedido(PedidoCanceladoDTO evento) {
        // Solo se repone si el pedido llegó a confirmarse. Si no hubo cobro,
        // el pedido se rechazó antes y el stock nunca se descontó: reponerlo
        // aquí inventaría existencias que nadie retiró.
        if (!evento.huboCobro()) {
            log.info("Pedido {} cancelado sin cobro: el stock no se había descontado",
                    evento.pedidoId());
            return;
        }

        log.info("Recibido pedido.cancelado para el pedido {}: reponiendo stock",
                evento.pedidoId());

        ajustarStock.reponer(evento.eventoId(), aItems(evento.items()));
    }

    private List<AjustarStockUseCase.ItemPedido> aItems(List<ItemEventoDTO> items) {
        return items.stream()
                .map(i -> new AjustarStockUseCase.ItemPedido(i.productoId(), i.cantidad()))
                .toList();
    }

    /** Último recurso: se agotaron los reintentos. */
    @DltHandler
    public void alAgotarseLosReintentos(ConsumerRecord<?, ?> registro) {
        dlq.registrar(registro);
    }
}
