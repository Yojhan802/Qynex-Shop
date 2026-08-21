package com.freestyleperu.aplicacion.configuracion.web;

import com.freestyleperu.aplicacion.configuracion.dto.request.ActualizarCompanySettingsRequest;
import com.freestyleperu.aplicacion.configuracion.dto.response.CompanySettingsResponse;
import com.freestyleperu.aplicacion.configuracion.dto.response.SystemInfoResponse;
import com.freestyleperu.aplicacion.configuracion.service.ConfiguracionService;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ConfiguracionController {

    private final ConfiguracionService configuracionService;

    public ConfiguracionController(ConfiguracionService configuracionService) {
        this.configuracionService = configuracionService;
    }

    @GetMapping("/api/settings/company")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_VER + "')")
    public CompanySettingsResponse obtener() {
        return configuracionService.obtener();
    }

    @PutMapping("/api/settings/company")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_EDITAR + "')")
    public CompanySettingsResponse actualizar(
            @Valid @RequestBody ActualizarCompanySettingsRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return configuracionService.actualizar(request, currentUser.id());
    }

    @PostMapping("/api/settings/company/logo")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_EDITAR + "')")
    public CompanySettingsResponse actualizarLogo(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return configuracionService.actualizarLogo(file, currentUser.id());
    }

    /**
     * Ficha pública mínima (nombre, plan, versión) para un panel externo de
     * monitoreo — sin autenticación, a propósito. Ver SystemInfoResponse.
     */
    @GetMapping("/api/system/info")
    public SystemInfoResponse infoSistema() {
        return configuracionService.obtenerInfoSistema();
    }
}
