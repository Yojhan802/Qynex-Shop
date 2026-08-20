package com.freestyleperu.aplicacion.usuario.web;

import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.usuario.dto.request.ActualizarRolRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.AsignarPermisosRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.CrearRolRequest;
import com.freestyleperu.aplicacion.usuario.dto.response.RolResponse;
import com.freestyleperu.aplicacion.usuario.service.RolService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('" + Permisos.ROLES_GESTIONAR + "')")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping("/api/roles")
    public List<RolResponse> listar() {
        return rolService.listar();
    }

    @GetMapping("/api/roles/{id}")
    public RolResponse obtener(@PathVariable Long id) {
        return rolService.obtener(id);
    }

    @PostMapping("/api/roles")
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody CrearRolRequest request) {
        RolResponse creado = rolService.crear(request);
        return ResponseEntity.created(URI.create("/api/roles/" + creado.id())).body(creado);
    }

    @PutMapping("/api/roles/{id}")
    public RolResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarRolRequest request) {
        return rolService.actualizar(id, request);
    }

    @PutMapping("/api/roles/{id}/permissions")
    public RolResponse actualizarPermisos(@PathVariable Long id, @Valid @RequestBody AsignarPermisosRequest request) {
        return rolService.actualizarPermisos(id, request);
    }
}
