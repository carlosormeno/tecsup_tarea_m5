package com.tecsup.app.micro.delivery.infrastructure.messaging.listener;

import com.tecsup.app.micro.delivery.application.AsignarEntregaUseCase;
import com.tecsup.app.micro.delivery.domain.exception.EntregaNoEncontradaException;
import com.tecsup.app.micro.delivery.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.delivery.infrastructure.messaging.Topics;
import com.tecsup.app.micro.delivery.infrastructure.messaging.dto.PedidoConfirmadoDTO;
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

/**
 * Adaptador de entrada: pedidos confirmados que hay que repartir.
 *
 * Nótese qué NO está en `exclude`: `SinRepartidoresException`. Es el único
 * fallo transitorio del servicio —un repartidor puede activarse en cualquier
 * momento—, así que se reintenta con retroceso exponencial en lugar de irse
 * directo a la DLQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventListener {

    private final AsignarEntregaUseCase asignarEntrega;
    private final DeadLetterQueue dlq;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            autoCreateTopics = "true",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    EntregaNoEncontradaException.class,
                    TransicionInvalidaException.class,
                    IllegalArgumentException.class
            })
    @KafkaListener(topics = Topics.PEDIDO_CONFIRMADO, groupId = "entregas-group")
    public void alConfirmarsePedido(PedidoConfirmadoDTO evento) {
        log.info("Recibido pedido.confirmado para el pedido {}: asignando repartidor",
                evento.pedidoId());

        asignarEntrega.asignar(evento.pedidoId(), evento.clienteId(), evento.direccionEntrega());
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
