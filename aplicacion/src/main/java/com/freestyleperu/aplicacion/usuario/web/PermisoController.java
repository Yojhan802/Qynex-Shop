package com.freestyleperu.aplicacion.usuario.web;

import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.usuario.dto.response.PermisoResponse;
import com.freestyleperu.aplicacion.usuario.mapper.PermisoMapper;
import com.freestyleperu.aplicacion.usuario.repository.PermisoRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermisoController {

    private final PermisoRepository permisoRepository;
    private final PermisoMapper permisoMapper;

    public PermisoController(PermisoRepository permisoRepository, PermisoMapper permisoMapper) {
        this.permisoRepository = permisoRepository;
        this.permisoMapper = permisoMapper;
    }

    @GetMapping("/api/permissions")
    @PreAuthorize("hasAuthority('" + Permisos.ROLES_GESTIONAR + "')")
    public List<PermisoResponse> listar() {
        return permisoRepository.findAllByOrderByModuleAscCodeAsc().stream().map(permisoMapper::toResponse).toList();
    }
}
