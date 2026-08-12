package com.tecsup.app.micro.user.infrastructure.persistence.adapter;

import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.domain.repository.UsuarioRepository;
import com.tecsup.app.micro.user.infrastructure.persistence.mapper.UsuarioPersistenceMapper;
import com.tecsup.app.micro.user.infrastructure.persistence.repository.JpaUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final JpaUsuarioRepository jpa;

    @Override
    public Usuario guardar(Usuario usuario) {
        return UsuarioPersistenceMapper.aDominio(
                jpa.save(UsuarioPersistenceMapper.aEntidad(usuario)));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return jpa.findById(id).map(UsuarioPersistenceMapper::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jpa.findByEmail(email).map(UsuarioPersistenceMapper::aDominio);
    }

    @Override
    public boolean existeEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public List<Usuario> buscarTodos() {
        return jpa.findAll().stream().map(UsuarioPersistenceMapper::aDominio).toList();
    }
}
