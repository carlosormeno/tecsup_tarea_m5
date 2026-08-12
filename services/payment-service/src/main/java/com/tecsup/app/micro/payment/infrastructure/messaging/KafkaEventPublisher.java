package com.tecsup.app.micro.payment.infrastructure.messaging;

import com.tecsup.app.micro.payment.domain.event.EventoDominio;
import com.tecsup.app.micro.payment.domain.event.PagoConfirmado;
import com.tecsup.app.micro.payment.domain.event.PagoRechazado;
import com.tecsup.app.micro.payment.domain.event.PublicadorEventos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adaptador de salida hacia Kafka.
 *
 * La correspondencia evento -> topic vive aquí, que es donde corresponde: el
 * nombre del topic es transporte. Un mapa en lugar de un if-else con
 * instanceof; si falta una entrada, el fallo es inmediato y explícito.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements PublicadorEventos {

    private static final Map<Class<? extends EventoDominio>, String> TOPIC_POR_EVENTO = Map.of(
            PagoConfirmado.class, Topics.PAGO_CONFIRMADO,
            PagoRechazado.class,  Topics.PAGO_RECHAZADO);

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

        // La clave es el id del PEDIDO: mantiene en orden todos los eventos
        // que le afectan, vengan de Pagos o de cualquier otro servicio.
        kafkaTemplate.send(topic, evento.idAgregado(), evento);
    }
}
