package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.domain.event.PedidoCancelado;
import com.tecsup.app.micro.order.domain.event.PedidoConfirmado;
import com.tecsup.app.micro.order.domain.event.PedidoEntregado;
import com.tecsup.app.micro.order.domain.model.EstadoEntrega;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: hacer avanzar la saga según lo que reportan Pagos y Entregas.
 *
 * Las tres reacciones viven juntas a propósito: comparten el mismo filtro de
 * idempotencia y forman una sola unidad de orquestación. Separarlas en tres
 * clases obligaría a duplicar ese filtro o a extraer un ayudante compartido,
 * y no ganaría nada: no son tres casos de uso distintos, son tres entradas
 * del mismo.
 *
 * Ver ADR-007 sobre la doble escritura BD/Kafka, igual que en CrearPedido.
 */
@Slf4j
@RequiredArgsConstructor
public class AvanzarSagaUseCaseImpl implements AvanzarSagaUseCase {

    private final PedidoRepository repositorio;
    private final PublicadorEventos publicador;

    @Override
    @Transactional
    public void pagoConfirmado(UUID pedidoId, String referenciaPago) {
        Pedido pedido = repositorio.obtener(pedidoId);

        if (yaProcesado(pedido, EstadoPedido.PAGADO, "pago.confirmado")) {
            return;
        }

        pedido.transicionarA(EstadoPedido.PAGADO, "Pago " + referenciaPago);
        repositorio.guardar(pedido);

        log.info("Pedido {} pagado (referencia {})", pedidoId, referenciaPago);
        publicador.publicar(PedidoConfirmado.de(pedido));
    }

    @Override
    @Transactional
    public void pagoRechazado(UUID pedidoId, String motivo) {
        Pedido pedido = repositorio.obtener(pedidoId);

        if (yaProcesado(pedido, EstadoPedido.RECHAZADO, "pago.rechazado")) {
            return;
        }

        pedido.transicionarA(EstadoPedido.RECHAZADO, motivo);
        repositorio.guardar(pedido);

        log.warn("Pedido {} rechazado: {}", pedidoId, motivo);
        // No hubo cobro, así que Pagos no tiene nada que reembolsar; el evento
        // se publica igual para que Catálogo sepa que no tocará stock.
        publicador.publicar(PedidoCancelado.de(pedido, motivo, false));
    }

    @Override
    @Transactional
    public void entregaCambioEstado(UUID pedidoId, EstadoEntrega estadoEntrega, String detalle) {
        Pedido pedido = repositorio.obtener(pedidoId);
        EstadoPedido destino = traducir(estadoEntrega);

        if (yaProcesado(pedido, destino, "entrega.estado-cambiado")) {
            return;
        }

        pedido.transicionarA(destino, detalle);
        repositorio.guardar(pedido);

        log.info("Pedido {} pasa a {} por la entrega ({})", pedidoId, destino, estadoEntrega);

        if (destino == EstadoPedido.ENTREGADO) {
            publicador.publicar(PedidoEntregado.de(pedido));
        } else if (destino == EstadoPedido.CANCELADO) {
            publicador.publicar(PedidoCancelado.de(pedido, detalle, true));
        }
    }

    private EstadoPedido traducir(EstadoEntrega estadoEntrega) {
        return switch (estadoEntrega) {
            case ASIGNADA -> EstadoPedido.EN_PREPARACION;
            case EN_CAMINO -> EstadoPedido.EN_CAMINO;
            case COMPLETADA -> EstadoPedido.ENTREGADO;
            case FALLIDA -> EstadoPedido.CANCELADO;
        };
    }

    /**
     * Filtro de idempotencia.
     *
     * Kafka entrega al menos una vez, así que un mismo evento puede llegar dos
     * veces tras un reintento. Si el pedido ya está en el estado que el evento
     * pretende provocar, es un duplicado y se ignora. Aquí basta con esto
     * porque la transición es idempotente por naturaleza; en Catálogo, en
     * cambio, descontar stock dos veces sí corrompe datos y hace falta una
     * tabla de eventos procesados.
     */
    private boolean yaProcesado(Pedido pedido, EstadoPedido destino, String evento) {
        if (pedido.estaEn(destino)) {
            log.info("Evento {} duplicado para el pedido {}: ya está en {}, se ignora",
                    evento, pedido.getId(), destino);
            return true;
        }
        return false;
    }
}
