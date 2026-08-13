package com.tecsup.app.micro.order.infrastructure.persistence.adapter;

import com.tecsup.app.micro.order.infrastructure.persistence.entity.PedidoJpaEntity;
import com.tecsup.app.micro.order.infrastructure.persistence.mapper.PedidoPersistenceMapper;
import com.tecsup.app.micro.order.infrastructure.persistence.repository.JpaPedidoRepository;
import com.tecsup.app.micro.order.domain.model.Pedido;
import com.tecsup.app.micro.order.domain.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto usando Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class PedidoRepositoryAdapter implements PedidoRepository {

    private final JpaPedidoRepository jpa;

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoJpaEntity guardada = jpa.save(PedidoPersistenceMapper.aEntidad(pedido));
        return PedidoPersistenceMapper.aDominio(guardada);
    }

    @Override
    public Optional<Pedido> buscarPorId(UUID id) {
        return jpa.findById(id).map(PedidoPersistenceMapper::aDominio);
    }

    @Override
    public List<Pedido> buscarPorCliente(Long clienteId) {
        return jpa.findByClienteIdOrderByCreadoEnDesc(clienteId).stream()
                .map(PedidoPersistenceMapper::aDominio)
                .toList();
    }

    @Override
    public List<Pedido> buscarTodos() {
        return jpa.findAllByOrderByCreadoEnDesc().stream()
                .map(PedidoPersistenceMapper::aDominio)
                .toList();
    }
}
