package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.event.PagoSolicitado;
import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: pagar un pedido. Aquí arranca la saga.
 *
 * La protección contra el doble clic no es un `if` ni una bandera: es la propia
 * máquina de estados. El primer intento mueve el pedido a `PAGO_EN_PROCESO`; el
 * segundo choca contra una transición que ya no existe y devuelve 409 sin haber
 * publicado nada. La regla vive en el dominio, no en el navegador.
 *
 * NOTA SOBRE CONSISTENCIA (ver ADR-007): guardar en Postgres y publicar en
 * Kafka son dos escrituras sin transacción común. Si la BD confirma y el broker
 * falla, el pedido se queda en `PAGO_EN_PROCESO` para siempre y la saga se
 * detiene. La solución formal es el patrón outbox; aquí se asume el riesgo de
 * forma consciente y se documenta.
 */
@Slf4j
@RequiredArgsConstructor
public class PagarPedidoUseCaseImpl implements PagarPedidoUseCase {

    private final PedidoRepository repositorio;
    private final PublicadorEventos publicador;

    @Override
    @Transactional
    public Pedido pagar(UUID pedidoId) {
        Pedido pedido = repositorio.obtener(pedidoId);

        pedido.transicionarA(EstadoPedido.PAGO_EN_PROCESO, "Pago solicitado por el cliente");
        repositorio.guardar(pedido);

        log.info("Pedido {} pasa a PAGO_EN_PROCESO por {}: se solicita el cobro",
                pedidoId, pedido.total());

        publicador.publicar(PagoSolicitado.de(pedido));
        return pedido;
    }
}
