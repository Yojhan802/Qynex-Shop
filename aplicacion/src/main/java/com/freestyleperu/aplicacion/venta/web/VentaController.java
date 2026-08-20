package com.freestyleperu.aplicacion.venta.web;

import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.venta.domain.SaleStatus;
import com.freestyleperu.aplicacion.venta.dto.request.AnularVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.request.CrearVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.response.VentaResponse;
import com.freestyleperu.aplicacion.venta.dto.response.VentaResumenResponse;
import com.freestyleperu.aplicacion.venta.service.VentaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping("/api/sales")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_CONSULTAR + "')")
    public PageResponse<VentaResumenResponse> listar(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) SaleStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable) {
        boolean verTodas = currentUser.tienePermiso(Permisos.VENTAS_CONSULTAR_TODAS);
        return ventaService.listar(currentUser.id(), verTodas, status, from, to, pageable);
    }

    @GetMapping("/api/sales/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_CONSULTAR + "')")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventaService.obtener(id);
    }

    @PostMapping("/api/sales")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_CREAR + "')")
    public ResponseEntity<VentaResponse> crear(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CrearVentaRequest request) {
        VentaResponse creada = ventaService.registrarVenta(request, currentUser.id(), currentUser.authorities());
        return ResponseEntity.created(URI.create("/api/sales/" + creada.id())).body(creada);
    }

    @PostMapping("/api/sales/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permisos.VENTAS_ANULAR + "')")
    public VentaResponse anular(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AnularVentaRequest request) {
        return ventaService.anular(id, request, currentUser.id());
    }
}
