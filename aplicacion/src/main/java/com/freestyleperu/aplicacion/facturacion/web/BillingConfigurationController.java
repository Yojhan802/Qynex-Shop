package com.freestyleperu.aplicacion.facturacion.web;

import com.freestyleperu.aplicacion.facturacion.dto.request.ActualizarBillingConfigurationRequest;
import com.freestyleperu.aplicacion.facturacion.dto.response.BillingConfigurationResponse;
import com.freestyleperu.aplicacion.facturacion.service.BillingConfigurationService;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BillingConfigurationController {

    private final BillingConfigurationService service;

    public BillingConfigurationController(BillingConfigurationService service) {
        this.service = service;
    }

    @GetMapping("/api/settings/billing")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_EDITAR + "')")
    public BillingConfigurationResponse obtener() {
        return service.obtener();
    }

    /**
     * Series que la empresa puede usar, para poblar los selectores del panel. Pasa por el
     * servidor y no por el navegador porque consultarlas exige las credenciales del
     * proveedor: un secreto en un bundle de JavaScript es un secreto público.
     */
    @GetMapping("/api/settings/billing/series")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_EDITAR + "')")
    public java.util.List<com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingProvider.SerieDisponible>
            series() {
        return service.seriesDisponibles();
    }

    @PutMapping("/api/settings/billing")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_EDITAR + "')")
    public BillingConfigurationResponse actualizar(@Valid @RequestBody ActualizarBillingConfigurationRequest request) {
        return service.actualizar(request);
    }
}
