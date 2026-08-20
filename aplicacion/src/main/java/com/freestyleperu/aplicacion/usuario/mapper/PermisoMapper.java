package com.freestyleperu.aplicacion.usuario.mapper;

import com.freestyleperu.aplicacion.usuario.domain.Permiso;
import com.freestyleperu.aplicacion.usuario.dto.response.PermisoResponse;
import org.springframework.stereotype.Component;

@Component
public class PermisoMapper {

    public PermisoResponse toResponse(Permiso permiso) {
        return new PermisoResponse(permiso.getId(), permiso.getCode(), permiso.getModule(), permiso.getDescription());
    }
}
