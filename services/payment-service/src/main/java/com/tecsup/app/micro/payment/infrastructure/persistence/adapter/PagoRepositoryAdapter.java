package com.tecsup.app.micro.payment.infrastructure.persistence.adapter;

import com.tecsup.app.micro.payment.domain.model.Pago;
import com.tecsup.app.micro.payment.domain.repository.PagoRepository;
import com.tecsup.app.micro.payment.infrastructure.persistence.mapper.PagoPersistenceMapper;
import com.tecsup.app.micro.payment.infrastructure.persistence.repository.JpaPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de salida: implementa el puerto usando Spring Data JPA. */
@Repository
@RequiredArgsConstructor
public class PagoRepositoryAdapter implements PagoRepository {

    private final JpaPagoRepository jpa;

    @Override
    public Pago guardar(Pago pago) {
        return PagoPersistenceMapper.aDominio(jpa.save(PagoPersistenceMapper.aEntidad(pago)));
    }

    @Override
    public Optional<Pago> buscarPorId(UUID id) {
        return jpa.findById(id).map(PagoPersistenceMapper::aDominio);
    }

    @Override
    public Optional<Pago> buscarPorPedido(UUID pedidoId) {
        return jpa.findByPedidoId(pedidoId).map(PagoPersistenceMapper::aDominio);
    }

    @Override
    public List<Pago> buscarTodos() {
        return jpa.findAll().stream().map(PagoPersistenceMapper::aDominio).toList();
    }
}
