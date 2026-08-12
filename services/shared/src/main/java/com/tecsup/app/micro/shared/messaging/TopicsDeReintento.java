package com.tecsup.app.micro.shared.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Arrays;
import java.util.List;

/**
 * Declara con el número correcto de particiones los topics que
 * {@code @RetryableTopic} genera por su cuenta.
 *
 * POR QUÉ EXISTE ESTA CLASE
 *
 * `@RetryableTopic` crea sus topics de reintento y su DLT con UNA partición,
 * mientras que los topics de negocio tienen tres. Y
 * `DeadLetterPublishingRecoverer` publica en la MISMA partición de la que vino
 * el mensaje: si venía de la partición 1 o 2, la DLT no la tiene y la
 * publicación falla.
 *
 * La consecuencia es peor que perder el mensaje: al fallar la publicación no
 * se confirma el offset, así que el consumidor vuelve a leer el mismo
 * registro una y otra vez. Un mensaje que nunca puede procesarse deja al
 * consumidor girando en vacío para siempre.
 *
 * Se detectó en ejecución: un evento en la partición 1 generó un bucle que
 * escribió 7 MB de log en tres minutos, mientras que otro idéntico en la
 * partición 0 sí llegó a su DLT. La DLQ funcionaba para uno de cada tres
 * mensajes.
 */
public final class TopicsDeReintento {

    /**
     * Nombres que genera Spring con `attempts = 4` y
     * `@Backoff(delay = 2000, multiplier = 2.0)`: 2s, 4s y 8s.
     */
    private static final List<String> SUFIJOS_REINTENTO =
            List.of("-retry-2000", "-retry-4000", "-retry-8000");

    private TopicsDeReintento() {
    }

    /**
     * Declara, para cada topic consumido: el propio topic, sus tres topics de
     * reintento y su DLT.
     *
     * POR QUÉ TAMBIÉN EL TOPIC BASE
     *
     * Un topic lo crea normalmente quien lo publica. Pero si el consumidor
     * arranca antes que el publicador, el topic no existe todavía y el
     * consumidor repite `UNKNOWN_TOPIC_OR_PARTITION` cada segundo hasta que
     * aparezca. Es recuperable —se conecta solo en cuanto exista— pero deja
     * el log inservible mientras tanto.
     *
     * Declarándolo también aquí, lo crea el primero que arranque. Como todos
     * los servicios usan el mismo número de particiones, la definición es
     * idéntica y el que llega después simplemente lo encuentra hecho.
     *
     * @param particiones      las mismas en todos los servicios
     * @param sufijoDlt        el sufijo propio del servicio, p. ej. "-pagos-dlt"
     * @param topicsConsumidos los topics que este servicio escucha
     */
    public static KafkaAdmin.NewTopics para(int particiones, short replicas,
                                            String sufijoDlt, String... topicsConsumidos) {

        NewTopic[] topics = Arrays.stream(topicsConsumidos)
                .flatMap(topic -> {
                    var derivados = new java.util.ArrayList<String>(SUFIJOS_REINTENTO.size() + 2);
                    derivados.add(topic);   // el topic base, por si el publicador aún no arrancó
                    SUFIJOS_REINTENTO.forEach(sufijo -> derivados.add(topic + sufijo));
                    derivados.add(topic + sufijoDlt);
                    return derivados.stream();
                })
                .map(nombre -> TopicBuilder.name(nombre)
                        .partitions(particiones)
                        .replicas(replicas)
                        .build())
                .toArray(NewTopic[]::new);

        return new KafkaAdmin.NewTopics(topics);
    }
}
