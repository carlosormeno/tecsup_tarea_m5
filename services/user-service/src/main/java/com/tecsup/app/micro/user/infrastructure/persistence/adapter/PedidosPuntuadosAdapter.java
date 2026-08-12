package com.tecsup.app.micro.user.infrastructure.persistence.adapter;

import com.tecsup.app.micro.user.domain.repository.PedidosPuntuados;
import com.tecsup.app.micro.user.infrastructure.persistence.entity.PedidoPuntuadoJpaEntity;
import com.tecsup.app.micro.user.infrastructure.persistence.repository.JpaPedidoPuntuadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Adaptador de la idempotencia de puntos.
 *
 * No abre transacción propia: se apoya en la del caso de uso, que es lo que
 * garantiza que registrar el pedido y sumar los puntos ocurran juntos.
 */
@Repository
@RequiredArgsConstructor
public class PedidosPuntuadosAdapter implements PedidosPuntuados {

    private final JpaPedidoPuntuadoRepository jpa;

    @Override
    public boolean yaPuntuado(UUID pedidoId) {
        return jpa.existsById(pedidoId);
    }

    @Override
    public void registrar(UUID pedidoId, Long usuarioId, int puntos) {
        jpa.save(new PedidoPuntuadoJpaEntity(pedidoId, usuarioId, puntos, Instant.now()));
    }
}
