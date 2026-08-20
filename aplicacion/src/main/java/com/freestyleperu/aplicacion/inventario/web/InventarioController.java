package com.freestyleperu.aplicacion.inventario.web;

import com.freestyleperu.aplicacion.inventario.domain.MovementType;
import com.freestyleperu.aplicacion.inventario.dto.request.AjusteInventarioRequest;
import com.freestyleperu.aplicacion.inventario.dto.request.EntradaInventarioRequest;
import com.freestyleperu.aplicacion.inventario.dto.request.SalidaInventarioRequest;
import com.freestyleperu.aplicacion.inventario.dto.response.InventoryItemResponse;
import com.freestyleperu.aplicacion.inventario.dto.response.MovimientoResponse;
import com.freestyleperu.aplicacion.inventario.service.InventarioService;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InventarioController {

    private final InventarioService inventarioService;

    public InventarioController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/api/inventory")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_CONSULTAR + "')")
    public PageResponse<InventoryItemResponse> listarStock(@RequestParam(required = false) String search, Pageable pageable) {
        return inventarioService.listarStock(search, pageable);
    }

    @GetMapping("/api/inventory/low-stock")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_CONSULTAR + "')")
    public List<InventoryItemResponse> listarStockBajo() {
        return inventarioService.listarStockBajo();
    }

    @GetMapping("/api/inventory/out-of-stock")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_CONSULTAR + "')")
    public List<InventoryItemResponse> listarAgotados() {
        return inventarioService.listarAgotados();
    }

    @GetMapping("/api/inventory/movements")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_CONSULTAR + "')")
    public PageResponse<MovimientoResponse> listarMovimientos(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable) {
        return inventarioService.listarMovimientos(variantId, type, from, to, pageable);
    }

    @PostMapping("/api/inventory/entry")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_ENTRADA + "')")
    public MovimientoResponse registrarEntrada(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody EntradaInventarioRequest request) {
        return inventarioService.registrarEntrada(request, currentUser.id());
    }

    @PostMapping("/api/inventory/exit")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_SALIDA + "')")
    public MovimientoResponse registrarSalida(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody SalidaInventarioRequest request) {
        return inventarioService.registrarSalida(request, currentUser.id());
    }

    @PostMapping("/api/inventory/adjustment")
    @PreAuthorize("hasAuthority('" + Permisos.INVENTARIO_AJUSTAR + "')")
    public MovimientoResponse registrarAjuste(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AjusteInventarioRequest request) {
        return inventarioService.registrarAjuste(request, currentUser.id());
    }
}
