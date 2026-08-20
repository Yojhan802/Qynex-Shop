package com.freestyleperu.aplicacion.reporte.web;

import com.freestyleperu.aplicacion.reporte.dto.response.CajaSesionResumenResponse;
import com.freestyleperu.aplicacion.reporte.dto.response.DashboardResponse;
import com.freestyleperu.aplicacion.reporte.dto.response.ProductoTopResponse;
import com.freestyleperu.aplicacion.reporte.dto.response.SerieDiaResponse;
import com.freestyleperu.aplicacion.reporte.dto.response.SerieEtiquetaResponse;
import com.freestyleperu.aplicacion.reporte.service.ReporteService;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('" + Permisos.REPORTES_CONSULTAR + "')")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/api/reports/dashboard")
    public DashboardResponse dashboard() {
        return reporteService.dashboard();
    }

    @GetMapping("/api/reports/sales/by-day")
    public List<SerieDiaResponse> ventasPorDia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reporteService.ventasPorDia(from, to);
    }

    @GetMapping("/api/reports/sales/by-category")
    public List<SerieEtiquetaResponse> ventasPorCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reporteService.ventasPorCategoria(from, to);
    }

    @GetMapping("/api/reports/sales/by-seller")
    public List<SerieEtiquetaResponse> ventasPorVendedor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reporteService.ventasPorVendedor(from, to);
    }

    @GetMapping("/api/reports/sales/by-payment-method")
    public List<SerieEtiquetaResponse> distribucionPorMetodoPago(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reporteService.distribucionPorMetodoPago(from, to);
    }

    @GetMapping("/api/reports/products/top-selling")
    public List<ProductoTopResponse> topProductos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return reporteService.topProductos(from, to, limit);
    }

    @GetMapping("/api/reports/cash/sessions")
    public List<CajaSesionResumenResponse> sesionesCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reporteService.sesionesCaja(from, to);
    }
}
