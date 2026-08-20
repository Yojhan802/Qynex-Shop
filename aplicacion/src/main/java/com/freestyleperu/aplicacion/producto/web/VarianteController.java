package com.freestyleperu.aplicacion.producto.web;

import com.freestyleperu.aplicacion.producto.dto.request.ActualizarVarianteRequest;
import com.freestyleperu.aplicacion.producto.dto.request.CrearVarianteRequest;
import com.freestyleperu.aplicacion.producto.dto.request.GenerarVariantesRequest;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteBusquedaResponse;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteResponse;
import com.freestyleperu.aplicacion.producto.service.VarianteService;
import com.freestyleperu.aplicacion.shared.dto.CambiarEstadoRequest;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VarianteController {

    private final VarianteService varianteService;

    public VarianteController(VarianteService varianteService) {
        this.varianteService = varianteService;
    }

    @GetMapping("/api/products/{productId}/variants")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_CONSULTAR + "')")
    public List<VarianteResponse> listarPorProducto(@PathVariable Long productId) {
        return varianteService.listarPorProducto(productId);
    }

    @PostMapping("/api/products/{productId}/variants")
    @PreAuthorize("hasAuthority('" + Permisos.VARIANTES_GESTIONAR + "')")
    public ResponseEntity<VarianteResponse> crear(@PathVariable Long productId, @Valid @RequestBody CrearVarianteRequest request) {
        VarianteResponse creado = varianteService.crear(productId, request);
        return ResponseEntity.created(URI.create("/api/variants/" + creado.id())).body(creado);
    }

    @PostMapping("/api/products/{productId}/variants/bulk")
    @PreAuthorize("hasAuthority('" + Permisos.VARIANTES_GESTIONAR + "')")
    public List<VarianteResponse> generarMatriz(@PathVariable Long productId, @Valid @RequestBody GenerarVariantesRequest request) {
        return varianteService.generarMatriz(productId, request);
    }

    @PutMapping("/api/variants/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.VARIANTES_GESTIONAR + "')")
    public VarianteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarVarianteRequest request) {
        return varianteService.actualizar(id, request);
    }

    @PatchMapping("/api/variants/{id}/status")
    @PreAuthorize("hasAuthority('" + Permisos.VARIANTES_GESTIONAR + "')")
    public VarianteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return varianteService.cambiarEstado(id, request.status());
    }

    @PostMapping("/api/variants/{id}/barcode")
    @PreAuthorize("hasAuthority('" + Permisos.BARCODE_GENERAR + "')")
    public VarianteResponse asignarCodigoBarras(@PathVariable Long id) {
        return varianteService.asignarCodigoBarras(id);
    }

    @GetMapping("/api/variants/barcode/{barcode}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_CONSULTAR + "')")
    public VarianteBusquedaResponse buscarPorCodigoBarras(@PathVariable String barcode) {
        return varianteService.buscarPorCodigoBarras(barcode);
    }

    @GetMapping("/api/variants/search")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_CONSULTAR + "')")
    public List<VarianteBusquedaResponse> buscar(@RequestParam String q) {
        return varianteService.buscar(q);
    }
}
