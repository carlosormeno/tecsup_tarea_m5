package com.tecsup.app.micro.order.infrastructure.messaging;

import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.domain.event.EventoDominio;
import com.tecsup.app.micro.order.domain.event.PedidoCancelado;
import com.tecsup.app.micro.order.domain.event.PedidoConfirmado;
import com.tecsup.app.micro.order.domain.event.PedidoCreado;
import com.tecsup.app.micro.order.domain.event.PedidoEntregado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adaptador de salida hacia Kafka.
 *
 * Aquí vive la decisión de a qué topic va cada evento, que es donde
 * corresponde: el nombre del topic es transporte. Un mapa y no una cadena de
 * if-else con instanceof — añadir un evento es una línea, y si se olvida, el
 * fallo es inmediato y explícito en vez de un mensaje que se pierde.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements PublicadorEventos {

    private static final Map<Class<? extends EventoDominio>, String> TOPIC_POR_EVENTO = Map.of(
            PedidoCreado.class,     Topics.PEDIDO_CREADO,
            PedidoConfirmado.class, Topics.PEDIDO_CONFIRMADO,
            PedidoEntregado.class,  Topics.PEDIDO_ENTREGADO,
            PedidoCancelado.class,  Topics.PEDIDO_CANCELADO);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publicar(EventoDominio evento) {
        String topic = TOPIC_POR_EVENTO.get(evento.getClass());

        if (topic == null) {
            throw new IllegalStateException(
                    "No hay topic configurado para " + evento.getClass().getName());
        }

        log.debug("Publicando {} en {} (clave {})",
                evento.getClass().getSimpleName(), topic, evento.idAgregado());

        // El id del agregado como clave: garantiza que todos los eventos de un
        // mismo pedido van a la misma partición y se consumen en orden.
        kafkaTemplate.send(topic, evento.idAgregado(), evento);
    }
}
