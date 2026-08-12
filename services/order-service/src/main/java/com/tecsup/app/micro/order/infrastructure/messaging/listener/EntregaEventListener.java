package com.tecsup.app.micro.order.infrastructure.messaging.listener;

import com.tecsup.app.micro.order.infrastructure.messaging.Topics;
import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.order.application.AvanzarSagaUseCase;
import com.tecsup.app.micro.order.infrastructure.messaging.dto.EntregaEstadoCambiadoDTO;
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

/** Adaptador de entrada: cambios de estado que reporta Entregas. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntregaEventListener {

    private final AvanzarSagaUseCase saga;
    private final DeadLetterQueue dlq;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            autoCreateTopics = "true",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    PedidoNoEncontradoException.class,
                    TransicionInvalidaException.class
            })
    @KafkaListener(topics = Topics.ENTREGA_ESTADO_CAMBIADO, groupId = "pedidos-group")
    public void alCambiarLaEntrega(EntregaEstadoCambiadoDTO evento) {
        log.info("Recibido entrega.estado-cambiado ({}) para el pedido {}",
                evento.nuevoEstado(), evento.pedidoId());

        saga.entregaCambioEstado(evento.pedidoId(), evento.nuevoEstado(), evento.detalle());
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
