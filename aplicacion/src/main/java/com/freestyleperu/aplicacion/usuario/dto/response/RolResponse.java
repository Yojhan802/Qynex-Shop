package com.freestyleperu.aplicacion.usuario.dto.response;

import java.util.List;

public record RolResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean isSystem,
        int hierarchyLevel,
        List<PermisoResponse> permisos) {
}
