package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import com.tecsup.app.micro.order.domain.event.PublicadorEventos;
import com.tecsup.app.micro.order.domain.event.PedidoCancelado;
import com.tecsup.app.micro.order.domain.model.EstadoPedido;
import com.tecsup.app.micro.order.domain.model.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Caso de uso: el cliente cancela su pedido y se dispara la compensación. */
@Slf4j
@RequiredArgsConstructor
public class CancelarPedidoUseCaseImpl implements CancelarPedidoUseCase {

    private final PedidoRepository repositorio;
    private final PublicadorEventos publicador;

    @Override
    @Transactional
    public Pedido cancelar(UUID pedidoId, String motivo) {
        Pedido pedido = repositorio.obtener(pedidoId);

        // Si el pedido llegó a PAGADO hay dinero cobrado y Pagos tendrá que
        // reembolsar. El evento lleva ese dato para que Pagos no tenga que
        // deducirlo por su cuenta. Se lee ANTES de la transición, claro: después
        // el estado ya es CANCELADO y no diría nada.
        boolean huboCobro = pedido.getEstado().implicaCobro();

        pedido.transicionarA(EstadoPedido.CANCELADO, motivo);
        repositorio.guardar(pedido);

        log.info("Pedido {} cancelado por el cliente: {}", pedidoId, motivo);
        publicador.publicar(PedidoCancelado.de(pedido, motivo, huboCobro));
        return pedido;
    }
}
