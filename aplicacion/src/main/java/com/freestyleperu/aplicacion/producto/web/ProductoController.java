package com.freestyleperu.aplicacion.producto.web;

import com.freestyleperu.aplicacion.producto.dto.request.ActualizarProductoRequest;
import com.freestyleperu.aplicacion.producto.dto.request.CrearProductoRequest;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoDetalleResponse;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoResumenResponse;
import com.freestyleperu.aplicacion.producto.service.ProductoService;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.dto.CambiarEstadoRequest;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import org.springframework.data.domain.Pageable;
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
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping("/api/products")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_CONSULTAR + "')")
    public PageResponse<ProductoResumenResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subcategoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) EstadoGeneral status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return PageResponse.of(
                productoService.listar(search, categoryId, subcategoryId, brandId, status, minPrice, maxPrice, pageable),
                it -> it);
    }

    @GetMapping("/api/products/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_CONSULTAR + "')")
    public ProductoDetalleResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping("/api/products")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_CREAR + "')")
    public ResponseEntity<ProductoDetalleResponse> crear(@Valid @RequestBody CrearProductoRequest request) {
        ProductoDetalleResponse creado = productoService.crear(request);
        return ResponseEntity.created(URI.create("/api/products/" + creado.id())).body(creado);
    }

    @PutMapping("/api/products/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_EDITAR + "')")
    public ProductoDetalleResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @PatchMapping("/api/products/{id}/status")
    @PreAuthorize("hasAuthority('" + Permisos.PRODUCTOS_EDITAR + "')")
    public ProductoResumenResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return productoService.cambiarEstado(id, request.status());
    }
}
