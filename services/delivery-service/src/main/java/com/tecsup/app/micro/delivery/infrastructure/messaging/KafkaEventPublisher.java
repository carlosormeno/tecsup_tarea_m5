package com.tecsup.app.micro.delivery.infrastructure.messaging;

import com.tecsup.app.micro.delivery.domain.event.EntregaEstadoCambiado;
import com.tecsup.app.micro.delivery.domain.event.EventoDominio;
import com.tecsup.app.micro.delivery.domain.event.PublicadorEventos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Adaptador de salida hacia Kafka. */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements PublicadorEventos {

    private static final Map<Class<? extends EventoDominio>, String> TOPIC_POR_EVENTO = Map.of(
            EntregaEstadoCambiado.class, Topics.ENTREGA_ESTADO_CAMBIADO);

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

        // La clave es el id del PEDIDO: mantiene en orden todos los cambios de
        // estado de una entrega, para que Pedidos no reciba COMPLETADA antes
        // que EN_CAMINO.
        kafkaTemplate.send(topic, evento.idAgregado(), evento);
    }
}
