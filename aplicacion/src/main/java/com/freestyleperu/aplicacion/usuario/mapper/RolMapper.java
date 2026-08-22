package com.freestyleperu.aplicacion.usuario.mapper;

import com.freestyleperu.aplicacion.usuario.domain.Permiso;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.dto.response.RolResponse;
import com.freestyleperu.aplicacion.usuario.dto.response.RolResumenResponse;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class RolMapper {

    private final PermisoMapper permisoMapper;

    public RolMapper(PermisoMapper permisoMapper) {
        this.permisoMapper = permisoMapper;
    }

    public RolResponse toResponse(Rol rol) {
        return new RolResponse(
                rol.getId(),
                rol.getCode(),
                rol.getName(),
                rol.getDescription(),
                rol.isSystem(),
                rol.getHierarchyLevel(),
                rol.getPermisos().stream()
                        .sorted(Comparator.comparing(Permiso::getCode))
                        .map(permisoMapper::toResponse)
                        .toList());
    }

    public RolResumenResponse toResumen(Rol rol) {
        return new RolResumenResponse(rol.getId(), rol.getCode(), rol.getName(), rol.getHierarchyLevel());
    }
}
