package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.model.Pago;
import com.tecsup.app.micro.payment.domain.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: compensación de la saga.
 *
 * Lo dispara `pedido.cancelado`. Los tres casos en que NO hay que devolver
 * nada se resuelven ignorando el evento en silencio, no lanzando:
 *
 *   - no existe pago      -> el pedido se canceló antes de llegar a cobrarse
 *   - el pago fue rechazado -> nunca se cobró
 *   - ya está reembolsado -> evento duplicado
 *
 * Esa tolerancia es lo que hace el caso de uso idempotente sin necesitar una
 * tabla de eventos procesados.
 */
@Slf4j
@RequiredArgsConstructor
public class ReembolsarPagoUseCaseImpl implements ReembolsarPagoUseCase {

    private final PagoRepository repositorio;

    @Override
    @Transactional
    public void reembolsarPorPedido(UUID pedidoId, String motivo) {
        Optional<Pago> encontrado = repositorio.buscarPorPedido(pedidoId);

        if (encontrado.isEmpty()) {
            log.info("El pedido {} se canceló sin que existiera pago; nada que reembolsar", pedidoId);
            return;
        }

        Pago pago = encontrado.get();
        if (!pago.fueAprobado()) {
            log.info("El pago del pedido {} está en {}; no procede reembolso",
                    pedidoId, pago.getEstado());
            return;
        }

        pago.reembolsar(motivo);
        repositorio.guardar(pago);

        log.info("Reembolsado el pago {} del pedido {}: {}",
                pago.getReferencia(), pedidoId, motivo);
    }
}
