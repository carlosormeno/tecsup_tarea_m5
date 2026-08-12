package com.tecsup.app.micro.delivery.infrastructure.persistence.adapter;

import com.tecsup.app.micro.delivery.domain.model.Entrega;
import com.tecsup.app.micro.delivery.domain.model.EstadoEntrega;
import com.tecsup.app.micro.delivery.domain.repository.EntregaRepository;
import com.tecsup.app.micro.delivery.infrastructure.persistence.mapper.EntregaPersistenceMapper;
import com.tecsup.app.micro.delivery.infrastructure.persistence.repository.JpaEntregaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EntregaRepositoryAdapter implements EntregaRepository {

    /** Una entrega "en curso" es la que todavía ocupa al repartidor. */
    private static final Set<EstadoEntrega> EN_CURSO =
            EnumSet.of(EstadoEntrega.ASIGNADA, EstadoEntrega.EN_CAMINO);

    private final JpaEntregaRepository jpa;

    @Override
    public Entrega guardar(Entrega entrega) {
        return EntregaPersistenceMapper.aDominio(
                jpa.save(EntregaPersistenceMapper.aEntidad(entrega)));
    }

    @Override
    public Optional<Entrega> buscarPorId(UUID id) {
        return jpa.findById(id).map(EntregaPersistenceMapper::aDominio);
    }

    @Override
    public Optional<Entrega> buscarPorPedido(UUID pedidoId) {
        return jpa.findByPedidoId(pedidoId).map(EntregaPersistenceMapper::aDominio);
    }

    @Override
    public List<Entrega> buscarTodas() {
        return jpa.findAll().stream().map(EntregaPersistenceMapper::aDominio).toList();
    }

    @Override
    public long contarEnCursoDe(Long repartidorId) {
        return jpa.countByRepartidorIdAndEstadoIn(repartidorId, EN_CURSO);
    }
}
