package com.freestyleperu.aplicacion.caja.web;

import com.freestyleperu.aplicacion.caja.domain.CashSessionStatus;
import com.freestyleperu.aplicacion.caja.dto.request.AbrirCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.request.CerrarCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.request.MovimientoCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.response.CajaResponse;
import com.freestyleperu.aplicacion.caja.dto.response.MovimientoCajaResponse;
import com.freestyleperu.aplicacion.caja.dto.response.ResumenCierreResponse;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaDetalleResponse;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaResponse;
import com.freestyleperu.aplicacion.caja.service.CajaService;
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
public class CajaController {

    private final CajaService cajaService;

    public CajaController(CajaService cajaService) {
        this.cajaService = cajaService;
    }

    @GetMapping("/api/cash-registers")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_CONSULTAR + "')")
    public List<CajaResponse> listarCajas() {
        return cajaService.listarCajas();
    }

    @GetMapping("/api/cash-registers/sessions")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_CONSULTAR + "')")
    public PageResponse<SesionCajaResponse> listarSesiones(
            @RequestParam(required = false) Long cashRegisterId,
            @RequestParam(required = false) CashSessionStatus status,
            Pageable pageable) {
        return cajaService.listarSesiones(cashRegisterId, status, pageable);
    }

    @GetMapping("/api/cash-registers/sessions/current")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_CONSULTAR + "')")
    public SesionCajaDetalleResponse obtenerSesionActual(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return cajaService.obtenerSesionActual(currentUser.id());
    }

    @GetMapping("/api/cash-registers/sessions/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_CONSULTAR + "')")
    public SesionCajaDetalleResponse obtenerSesion(@PathVariable Long id) {
        return cajaService.obtenerSesion(id);
    }

    @GetMapping("/api/cash-registers/sessions/{id}/summary")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_CONSULTAR + "')")
    public ResumenCierreResponse obtenerResumenCierre(@PathVariable Long id) {
        return cajaService.obtenerResumenCierre(id);
    }

    @PostMapping("/api/cash-registers/sessions")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_ABRIR + "')")
    public ResponseEntity<SesionCajaResponse> abrirCaja(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AbrirCajaRequest request) {
        SesionCajaResponse creada = cajaService.abrirCaja(request, currentUser.id());
        return ResponseEntity.created(URI.create("/api/cash-registers/sessions/" + creada.id())).body(creada);
    }

    @PostMapping("/api/cash-registers/sessions/{id}/close")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_CERRAR + "')")
    public SesionCajaResponse cerrarCaja(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CerrarCajaRequest request) {
        return cajaService.cerrarCaja(id, request, currentUser.id());
    }

    @PostMapping("/api/cash-registers/movements")
    @PreAuthorize("hasAuthority('" + Permisos.CAJA_MOVIMIENTO + "')")
    public MovimientoCajaResponse registrarMovimiento(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody MovimientoCajaRequest request) {
        return cajaService.registrarMovimiento(request, currentUser.id());
    }
}
