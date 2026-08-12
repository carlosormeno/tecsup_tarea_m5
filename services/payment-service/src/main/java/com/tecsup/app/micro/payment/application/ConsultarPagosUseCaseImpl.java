package com.tecsup.app.micro.payment.application;

import com.tecsup.app.micro.payment.domain.exception.PagoNoEncontradoException;
import com.tecsup.app.micro.payment.domain.model.Pago;
import com.tecsup.app.micro.payment.domain.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarPagosUseCaseImpl implements ConsultarPagosUseCase {

    private final PagoRepository repositorio;

    @Override
    @Transactional(readOnly = true)
    public Pago porPedido(UUID pedidoId) {
        return repositorio.buscarPorPedido(pedidoId)
                .orElseThrow(() -> new PagoNoEncontradoException("el pedido " + pedidoId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pago> todos() {
        return repositorio.buscarTodos();
    }
}
