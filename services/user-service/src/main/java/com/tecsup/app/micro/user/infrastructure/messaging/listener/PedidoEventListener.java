package com.tecsup.app.micro.user.infrastructure.messaging.listener;

import com.tecsup.app.micro.user.application.SumarPuntosUseCase;
import com.tecsup.app.micro.user.domain.exception.UsuarioNoEncontradoException;
import com.tecsup.app.micro.user.infrastructure.messaging.Topics;
import com.tecsup.app.micro.user.infrastructure.messaging.dto.PedidoEntregadoDTO;
import com.tecsup.app.micro.shared.dlq.DeadLetterQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: pedidos entregados que otorgan puntos. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoEventListener {

    private final SumarPuntosUseCase sumarPuntos;
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
                    UsuarioNoEncontradoException.class,
                    IllegalArgumentException.class
            })
    @KafkaListener(topics = Topics.PEDIDO_ENTREGADO, groupId = "usuarios-group")
    public void alEntregarsePedido(PedidoEntregadoDTO evento) {
        log.info("Recibido pedido.entregado del pedido {}: sumando puntos al cliente {}",
                evento.pedidoId(), evento.clienteId());

        sumarPuntos.porPedidoEntregado(evento.pedidoId(), evento.clienteId(), evento.total());
    }

    /** Último recurso: se agotaron los reintentos. */
    @DltHandler
    public void alAgotarseLosReintentos(ConsumerRecord<?, ?> registro) {
        dlq.registrar(registro);
    }
}
