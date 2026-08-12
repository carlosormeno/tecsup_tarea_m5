package com.tecsup.app.micro.delivery.application;

import com.tecsup.app.micro.delivery.domain.exception.EntregaNoEncontradaException;
import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.repository.EntregaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarEntregasUseCaseImpl implements ConsultarEntregasUseCase {

    private final EntregaRepository entregas;

    @Override
    @Transactional(readOnly = true)
    public Entrega porPedido(UUID pedidoId) {
        return entregas.buscarPorPedido(pedidoId).orElseThrow(
                () -> new EntregaNoEncontradaException("el pedido " + pedidoId));
    }

    @Override
    @Transactional(readOnly = true)
    public Entrega porId(UUID entregaId) {
        return entregas.obtener(entregaId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrega> todas() {
        return entregas.buscarTodas();
    }
}
