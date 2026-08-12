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
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
            autoCreateTopics = "true",
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
            autoCreateTopics = "true",
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

    @DltHandler
    public void alAgotarseLosReintentos(
            Object evento,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) String topicOrigen,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false) byte[] offsetOrigen,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String error) {

        dlq.registrarDesdeDlt(evento, topicOrigen, offsetOrigen, error);
    }
}
