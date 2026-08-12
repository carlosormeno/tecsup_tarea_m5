package com.tecsup.app.micro.shared.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueue {

    private final FailedEventRepository repositorio;
    private final ObjectMapper objectMapper;

    public void registrar(Object evento, String topicOrigen, Long offsetOrigen, String mensajeError) {
        FailedEvent fallido = FailedEvent.builder()
                .topicOrigen(topicOrigen)
                .offsetOrigen(offsetOrigen)
                .payload(serializar(evento))
                .mensajeError(recortar(mensajeError))
                .ocurridoEn(Instant.now())
                .build();

        repositorio.save(fallido);

        log.error("Evento de {} (offset {}) archivado en la DLQ: {}",
                topicOrigen, offsetOrigen, mensajeError);
    }

    public void registrarDesdeDlt(Object evento, String topicOrigen, byte[] offsetOrigen, String mensajeError) {
        Long offset = (offsetOrigen != null && offsetOrigen.length == Long.BYTES)
                ? ByteBuffer.wrap(offsetOrigen).getLong()
                : null;

        registrar(evento, topicOrigen != null ? topicOrigen : "(desconocido)", offset, mensajeError);
    }

    public List<FailedEvent> listar() {
        return repositorio.findAll();
    }

    private String serializar(Object evento) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (Exception e) {
            log.warn("No se pudo serializar el evento fallido: {}", e.getMessage());
            return String.valueOf(evento);
        }
    }

    private String recortar(String mensaje) {
        if (mensaje == null) {
            return "(sin mensaje)";
        }
        return mensaje.length() > 1000 ? mensaje.substring(0, 1000) : mensaje;
    }
}
