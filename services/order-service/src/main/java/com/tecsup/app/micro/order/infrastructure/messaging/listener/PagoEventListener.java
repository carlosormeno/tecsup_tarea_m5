package com.tecsup.app.micro.order.infrastructure.messaging.listener;

import com.tecsup.app.micro.order.infrastructure.messaging.Topics;
import com.tecsup.app.micro.order.domain.exception.PedidoNoEncontradoException;
import com.tecsup.app.micro.order.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.order.application.AvanzarSagaUseCase;
import com.tecsup.app.micro.order.infrastructure.messaging.dto.PagoConfirmadoDTO;
import com.tecsup.app.micro.order.infrastructure.messaging.dto.PagoRechazadoDTO;
import com.tecsup.app.micro.order.infrastructure.dlq.DeadLetterQueue;
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
 * Adaptador de entrada: eventos de Pagos.
 *
 * Solo traduce del mundo de Kafka al puerto de la saga. Ni una regla de
 * negocio vive aquí.
 *
 * Sobre la política de reintentos:
 *  - attempts = 4 son 4 entregas en total (la original más 3 reintentos). Con
 *    el 2 del ejemplo del curso solo había un reintento y el multiplicador del
 *    backoff nunca llegaba a aplicarse.
 *  - exclude lista los fallos deterministas: si el pedido no existe o la
 *    transición es imposible, reintentar tres veces da exactamente el mismo
 *    error. Van directo a la DLT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagoEventListener {

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
    @KafkaListener(topics = Topics.PAGO_CONFIRMADO, groupId = "pedidos-group")
    public void alConfirmarsePago(PagoConfirmadoDTO evento) {
        log.info("Recibido pago.confirmado para el pedido {}", evento.pedidoId());
        saga.pagoConfirmado(evento.pedidoId(), evento.referenciaPago());
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 2000, multiplier = 2.0),
            autoCreateTopics = "true",
            dltTopicSuffix = Topics.SUFIJO_DLT,
            exclude = {
                    PedidoNoEncontradoException.class,
                    TransicionInvalidaException.class
            })
    @KafkaListener(topics = Topics.PAGO_RECHAZADO, groupId = "pedidos-group")
    public void alRechazarsePago(PagoRechazadoDTO evento) {
        log.info("Recibido pago.rechazado para el pedido {}: {}", evento.pedidoId(), evento.motivo());
        saga.pagoRechazado(evento.pedidoId(), evento.motivo());
    }

    /** Último recurso: se agotaron los reintentos. */
    @DltHandler
    public void alAgotarseLosReintentos(
            Object evento,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_TOPIC, required = false) String topicOrigen,
            @Header(name = KafkaHeaders.DLT_ORIGINAL_OFFSET, required = false) byte[] offsetOrigen,
            @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false) String error) {

        dlq.registrarDesdeDlt(evento, topicOrigen, offsetOrigen, error);
    }
}
