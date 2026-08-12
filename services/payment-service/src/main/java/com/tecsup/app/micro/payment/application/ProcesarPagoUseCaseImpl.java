package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.event.PagoConfirmado;
import com.tecsup.app.micro.payment.domain.event.PagoRechazado;
import com.tecsup.app.micro.payment.domain.event.PublicadorEventos;
import com.tecsup.app.micro.payment.domain.model.Pago;
import com.tecsup.app.micro.payment.domain.repository.PagoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: cobrar el pedido y anunciar el resultado.
 *
 * Es el primer tramo de la saga: cualquiera de los dos caminos publica un
 * evento, y Pedidos reacciona pasando a PAGADO o a RECHAZADO. Nunca puede
 * terminar sin publicar nada, porque entonces la saga se quedaría colgada.
 *
 * Sobre consistencia (ADR-007): guardar en Postgres y publicar en Kafka son
 * dos escrituras sin transacción común. Se asume el riesgo y se documenta.
 */
@Slf4j
public class ProcesarPagoUseCaseImpl implements ProcesarPagoUseCase {

    private final PagoRepository repositorio;
    private final PublicadorEventos publicador;
    private final BigDecimal limiteAutorizado;

    public ProcesarPagoUseCaseImpl(PagoRepository repositorio, PublicadorEventos publicador,
                                   BigDecimal limiteAutorizado) {
        this.repositorio = repositorio;
        this.publicador = publicador;
        this.limiteAutorizado = limiteAutorizado;
    }

    @Override
    @Transactional
    public Pago procesar(UUID pedidoId, Long clienteId, BigDecimal monto) {
        // Idempotencia: Kafka entrega al menos una vez, y cobrar dos veces el
        // mismo pedido sí corrompe datos. A diferencia de Pedidos, aquí no
        // basta con que la operación sea idempotente por naturaleza.
        Optional<Pago> existente = repositorio.buscarPorPedido(pedidoId);
        if (existente.isPresent()) {
            log.info("El pedido {} ya tenía un pago ({}), se ignora el evento duplicado",
                    pedidoId, existente.get().getEstado());
            return existente.get();
        }

        Pago pago = repositorio.guardar(
                Pago.procesar(pedidoId, clienteId, monto, limiteAutorizado));

        if (pago.fueAprobado()) {
            log.info("Pago {} aprobado para el pedido {} por {}",
                    pago.getReferencia(), pedidoId, monto);
            publicador.publicar(PagoConfirmado.de(pago));
        } else {
            log.warn("Pago rechazado para el pedido {}: {}", pedidoId, pago.getMotivo());
            publicador.publicar(PagoRechazado.de(pago));
        }

        return pago;
    }
}
