package com.tecsup.app.micro.delivery.infrastructure.messaging.listener;

import com.tecsup.app.micro.delivery.application.AsignarEntregaUseCase;
import com.tecsup.app.micro.delivery.domain.exception.EntregaNoEncontradaException;
import com.tecsup.app.micro.delivery.domain.exception.TransicionInvalidaException;
import com.tecsup.app.micro.delivery.infrastructure.messaging.Topics;
import com.tecsup.app.micro.delivery.infrastructure.messaging.dto.PedidoConfirmadoDTO;
import com.tecsup.app.micro.shared.dlq.DeadLetterQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
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
            // false: los topics de reintento y DLT los declara KafkaTopicsConfig
            // con 3 particiones. Si los creara la anotación, los haría con una
            // sola y la publicación a la DLT fallaría desde las particiones 1 y 2.
            autoCreateTopics = "false",
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

    /** Último recurso: se agotaron los reintentos. */
    @DltHandler
    public void alAgotarseLosReintentos(ConsumerRecord<?, ?> registro) {
        dlq.registrar(registro);
    }
}
