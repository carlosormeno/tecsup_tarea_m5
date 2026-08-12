package com.tecsup.app.micro.shared.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Registro de eventos que no se pudieron procesar.
 *
 * Kafka ya deja el mensaje en el topic `-dlt` del servicio; esta clase además
 * lo persiste para poder consultarlo por REST y para que sobreviva a la
 * retención del broker.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueue {

    private final FailedEventRepository repositorio;
    private final ObjectMapper objectMapper;

    /**
     * Archiva un mensaje que llegó a la cola de fallidos.
     *
     * Recibe el ConsumerRecord completo en lugar del payload y unas cabeceras
     * sueltas. La razón es práctica: si el método del @DltHandler declara el
     * payload como `Object`, Spring le entrega el propio ConsumerRecord —que
     * también es un Object— y las cabeceras inyectadas con @Header llegan
     * nulas. Tomando el registro se obtiene todo de una fuente fiable, y si
     * alguna cabecera faltara quedan los datos del propio registro como
     * respaldo.
     */
    public void registrar(ConsumerRecord<?, ?> registro) {
        // OJO CON LOS NOMBRES DE LAS CABECERAS
        //
        // `@RetryableTopic` escribe la familia `kafka_original-*` y
        // `kafka_exception-*` (constantes ORIGINAL_TOPIC, EXCEPTION_MESSAGE).
        // Las constantes DLT_ORIGINAL_* corresponden a OTROS nombres
        // (`kafka_dlt-original-*`) que esta ruta no usa. Usar las equivocadas
        // no da error: simplemente devuelve null y la cola queda llena de
        // entradas sin diagnóstico. Se buscan ambas por si cambia la ruta.
        String topicOrigen = primeraCabecera(registro,
                KafkaHeaders.ORIGINAL_TOPIC, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        Long offsetOrigen = primeraCabeceraLong(registro,
                KafkaHeaders.ORIGINAL_OFFSET, KafkaHeaders.DLT_ORIGINAL_OFFSET);
        String mensaje = primeraCabecera(registro,
                KafkaHeaders.EXCEPTION_MESSAGE, KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        // La CAUSA antes que el envoltorio: Spring envuelve el fallo del
        // listener en ListenerExecutionFailedException, que no dice nada.
        // Lo útil para diagnosticar es la excepción de dominio de debajo.
        String tipo = primeraCabecera(registro,
                "kafka_exception-cause-fqcn",
                KafkaHeaders.EXCEPTION_FQCN, KafkaHeaders.DLT_EXCEPTION_FQCN);
        // `retry_topic-attempts` es un entero BINARIO de 4 bytes, no texto.
        // Leerlo como cadena mete bytes nulos en el mensaje, y Postgres
        // rechaza el INSERT con "invalid byte sequence for encoding UTF8".
        // Al fallar el guardado, el manejador lanza, el mensaje no se
        // confirma y el consumidor entra en bucle: el mismo desenlace de
        // siempre cuando algo falla en el último recurso.
        Integer intentos = cabeceraInt(registro, "retry_topic-attempts");

        // Si aun así faltan, deja constancia de las que SÍ vinieron: sin esto
        // solo se ve un "(desconocido)" sin pista de por qué.
        if (topicOrigen == null || mensaje == null) {
            log.warn("Faltan cabeceras DLT en el mensaje de {}. Presentes: {}",
                    registro.topic(), nombresDeCabeceras(registro));
        }

        FailedEvent fallido = FailedEvent.builder()
                .topicOrigen(limpiar(topicOrigen != null ? topicOrigen : registro.topic()))
                .offsetOrigen(offsetOrigen != null ? offsetOrigen : registro.offset())
                .payload(limpiar(serializar(registro.value())))
                .mensajeError(recortar(limpiar(describir(tipo, mensaje, intentos))))
                .ocurridoEn(Instant.now())
                .build();

        repositorio.save(fallido);

        log.error("Evento de {} (offset {}) archivado en la DLQ: {}",
                fallido.getTopicOrigen(), fallido.getOffsetOrigen(), fallido.getMensajeError());
    }

    /** Junta tipo de excepción, mensaje e intentos en una sola línea legible. */
    private String describir(String tipo, String mensaje, Integer intentos) {
        if (mensaje == null && tipo == null) {
            return "(sin mensaje de error)";
        }
        String simple = tipo == null ? "" : tipo.substring(tipo.lastIndexOf('.') + 1) + ": ";
        String sufijo = intentos == null ? "" : " [intentos: " + intentos + "]";
        // Fuera el prefijo del envoltorio de Spring, que solo estorba
        String limpio = mensaje == null ? "" : mensaje.replace("Listener failed; ", "");
        return simple + limpio + sufijo;
    }

    /**
     * Quita bytes nulos y caracteres de control antes de guardar.
     *
     * Las cabeceras de Kafka son binarias: algunas llevan texto y otras
     * enteros. Si una binaria acaba interpretada como cadena, el texto
     * resultante contiene 0x00 y Postgres rechaza la fila entera. Esta
     * limpieza es la red de seguridad para que un dato inesperado en una
     * cabecera nunca pueda tumbar el registro del fallo.
     */
    private String limpiar(String texto) {
        return texto == null ? null : texto.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
    }

    public List<FailedEvent> listar() {
        return repositorio.findAll();
    }

    private String nombresDeCabeceras(ConsumerRecord<?, ?> registro) {
        return StreamSupport.stream(registro.headers().spliterator(), false)
                .map(Header::key)
                .collect(Collectors.joining(", "));
    }

    private String cabecera(ConsumerRecord<?, ?> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private String primeraCabecera(ConsumerRecord<?, ?> registro, String... nombres) {
        for (String nombre : nombres) {
            String valor = cabecera(registro, nombre);
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    /** El offset original viaja como 8 bytes crudos, no como texto. */
    private Long cabeceraLong(ConsumerRecord<?, ?> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null || h.value().length != Long.BYTES) {
            return null;
        }
        return ByteBuffer.wrap(h.value()).getLong();
    }

    /** Entero binario de 4 bytes, como el contador de intentos. */
    private Integer cabeceraInt(ConsumerRecord<?, ?> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null || h.value().length != Integer.BYTES) {
            return null;
        }
        return ByteBuffer.wrap(h.value()).getInt();
    }

    private Long primeraCabeceraLong(ConsumerRecord<?, ?> registro, String... nombres) {
        for (String nombre : nombres) {
            Long valor = cabeceraLong(registro, nombre);
            if (valor != null) {
                return valor;
            }
        }
        return null;
    }

    /**
     * Si el evento no se puede serializar, se guarda su toString() en vez de
     * dejar caer el registro. Perder el evento fallido justo en el manejador
     * de fallos es el peor momento posible para quedarse sin información.
     */
    private String serializar(Object evento) {
        if (evento == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (Exception e) {
            log.warn("No se pudo serializar el evento fallido: {}", e.getMessage());
            return String.valueOf(evento);
        }
    }

    private String recortar(String mensaje) {
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
