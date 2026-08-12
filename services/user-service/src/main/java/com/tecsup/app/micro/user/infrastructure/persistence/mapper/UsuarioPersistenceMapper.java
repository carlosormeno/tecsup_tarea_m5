package com.tecsup.app.micro.user.infrastructure.persistence.mapper;

import com.tecsup.app.micro.user.domain.model.Rol;
import com.tecsup.app.micro.user.domain.model.Usuario;
import com.tecsup.app.micro.user.infrastructure.persistence.entity.UsuarioJpaEntity;

import java.util.EnumSet;

/** Traduce entre el agregado de dominio y la entidad JPA. */
public final class UsuarioPersistenceMapper {

    private UsuarioPersistenceMapper() {
    }

    public static UsuarioJpaEntity aEntidad(Usuario usuario) {
        return new UsuarioJpaEntity(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getPasswordHash(),
                usuario.getDireccion(),
                usuario.getPuntosFidelidad(),
                usuario.isActivo(),
                usuario.getCreadoEn(),
                EnumSet.copyOf(usuario.getRoles()));
    }

    public static Usuario aDominio(UsuarioJpaEntity entidad) {
        return Usuario.reconstituir(
                entidad.getId(),
                entidad.getNombre(),
                entidad.getEmail(),
                entidad.getPasswordHash(),
                entidad.getRoles().isEmpty() ? EnumSet.of(Rol.CLIENTE) : entidad.getRoles(),
                entidad.getDireccion(),
                entidad.getPuntosFidelidad(),
                entidad.isActivo(),
                entidad.getCreadoEn());
    }
}
