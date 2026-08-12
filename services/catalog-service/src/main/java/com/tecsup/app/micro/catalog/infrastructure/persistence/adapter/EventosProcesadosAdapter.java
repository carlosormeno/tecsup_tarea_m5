package com.tecsup.app.micro.catalog.infrastructure.persistence.adapter;

import com.tecsup.app.micro.catalog.domain.repository.EventosProcesados;
import com.tecsup.app.micro.catalog.infrastructure.persistence.entity.EventoProcesadoJpaEntity;
import com.tecsup.app.micro.catalog.infrastructure.persistence.repository.JpaEventoProcesadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Adaptador de la idempotencia.
 *
 * No abre transacción propia: se apoya en la del caso de uso, que es lo que
 * garantiza que marcar el evento y ajustar el stock ocurran juntos o no
 * ocurran.
 */
@Repository
@RequiredArgsConstructor
public class EventosProcesadosAdapter implements EventosProcesados {

    private final JpaEventoProcesadoRepository jpa;

    @Override
    public boolean yaProcesado(String eventoId) {
        return jpa.existsById(eventoId);
    }

    @Override
    public void marcarProcesado(String eventoId, String topic) {
        jpa.save(new EventoProcesadoJpaEntity(eventoId, topic, Instant.now()));
    }
}
