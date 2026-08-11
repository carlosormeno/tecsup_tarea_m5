package com.tecsup.app.micro.order.application;

import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import com.tecsup.app.micro.order.domain.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Caso de uso: consultas de solo lectura sobre pedidos. */
@RequiredArgsConstructor
public class ConsultarPedidosUseCaseImpl implements ConsultarPedidosUseCase {

    private final PedidoRepository repositorio;

    @Override
    @Transactional(readOnly = true)
    public Pedido porId(UUID pedidoId) {
        return repositorio.obtener(pedidoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> porCliente(Long clienteId) {
        return repositorio.buscarPorCliente(clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> todos() {
        return repositorio.buscarTodos();
    }
}
