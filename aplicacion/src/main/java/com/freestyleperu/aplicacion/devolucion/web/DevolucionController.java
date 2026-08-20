package com.freestyleperu.aplicacion.devolucion.web;

import com.freestyleperu.aplicacion.devolucion.dto.request.CrearDevolucionRequest;
import com.freestyleperu.aplicacion.devolucion.dto.response.DevolucionResponse;
import com.freestyleperu.aplicacion.devolucion.dto.response.ReturnableItemResponse;
import com.freestyleperu.aplicacion.devolucion.service.DevolucionService;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DevolucionController {

    private final DevolucionService devolucionService;

    public DevolucionController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    @GetMapping("/api/returns")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_CONSULTAR + "')")
    public PageResponse<DevolucionResponse> listar(@RequestParam(required = false) Long saleId, Pageable pageable) {
        return devolucionService.listar(saleId, pageable);
    }

    @GetMapping("/api/returns/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_CONSULTAR + "')")
    public DevolucionResponse obtener(@PathVariable Long id) {
        return devolucionService.obtener(id);
    }

    @GetMapping("/api/sales/{id}/returnable-items")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_DEVOLVER + "')")
    public List<ReturnableItemResponse> itemsDevolvibles(@PathVariable Long id) {
        return devolucionService.itemsDevolvibles(id);
    }

    @PostMapping("/api/returns")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_DEVOLVER + "')")
    public ResponseEntity<DevolucionResponse> registrar(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CrearDevolucionRequest request) {
        DevolucionResponse creada = devolucionService.registrar(request, currentUser.id());
        return ResponseEntity.created(URI.create("/api/returns/" + creada.id())).body(creada);
    }
}
