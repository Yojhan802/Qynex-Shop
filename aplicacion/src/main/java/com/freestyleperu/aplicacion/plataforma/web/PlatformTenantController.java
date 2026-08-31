package com.freestyleperu.aplicacion.plataforma.web;

import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import com.freestyleperu.aplicacion.plataforma.dto.request.ActualizarTenantRequest;
import com.freestyleperu.aplicacion.plataforma.dto.request.CrearTenantRequest;
import com.freestyleperu.aplicacion.plataforma.dto.response.CrearTenantResponse;
import com.freestyleperu.aplicacion.plataforma.dto.response.TenantResponse;
import com.freestyleperu.aplicacion.plataforma.service.PlatformTenantService;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/tenants")
@PreAuthorize("hasAuthority('" + Permisos.PLATAFORMA_EMPRESAS_GESTIONAR + "')")
public class PlatformTenantController {

    private final PlatformTenantService service;

    public PlatformTenantController(PlatformTenantService service) {
        this.service = service;
    }

    @GetMapping
    public List<TenantResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) SubscriptionStatus status) {
        return service.listar(search, status);
    }

    @PostMapping
    public CrearTenantResponse crear(@Valid @RequestBody CrearTenantRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.crear(request, currentUser.id());
    }

    @PutMapping("/{tenantId}")
    public TenantResponse actualizar(@PathVariable Long tenantId, @Valid @RequestBody ActualizarTenantRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.actualizar(tenantId, request, currentUser.id());
    }
}
