package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.event.EntregaEstadoCambiado;
import com.tecsup.app.micro.delivery.domain.event.PublicadorEventos;
import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import com.tecsup.app.micro.delivery.domain.repository.EntregaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: avanzar el estado de la entrega y anunciarlo.
 *
 * Cada cambio publica un evento, y es así como Pedidos va avanzando su propia
 * máquina de estados hasta ENTREGADO. Si este servicio dejara de publicar, el
 * pedido se quedaría clavado en PAGADO para siempre.
 */
@Slf4j
@RequiredArgsConstructor
public class ActualizarEntregaUseCaseImpl implements ActualizarEntregaUseCase {

    private final EntregaRepository entregas;
    private final PublicadorEventos publicador;

    @Override
    @Transactional
    public Entrega cambiarEstado(UUID entregaId, EstadoEntrega nuevoEstado, String detalle) {
        Entrega entrega = entregas.obtener(entregaId);

        // Idempotencia: si ya está en ese estado, el cambio es un duplicado.
        // Se ignora en silencio en vez de fallar por transición inválida.
        if (entrega.estaEn(nuevoEstado)) {
            log.info("La entrega {} ya estaba en {}; no se republica", entregaId, nuevoEstado);
            return entrega;
        }

        entrega.cambiarEstado(nuevoEstado, detalle);
        entregas.guardar(entrega);

        log.info("Entrega {} del pedido {} pasa a {}",
                entregaId, entrega.getPedidoId(), nuevoEstado);

        publicador.publicar(EntregaEstadoCambiado.de(entrega));
        return entrega;
    }
}
