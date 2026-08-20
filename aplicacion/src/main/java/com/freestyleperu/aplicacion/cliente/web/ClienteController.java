package com.freestyleperu.aplicacion.cliente.web;

import com.freestyleperu.aplicacion.cliente.dto.request.ClienteRequest;
import com.freestyleperu.aplicacion.cliente.dto.response.ClienteResponse;
import com.freestyleperu.aplicacion.cliente.service.ClienteService;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.dto.CambiarEstadoRequest;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/api/customers")
    @PreAuthorize("hasAuthority('" + Permisos.CLIENTES_CONSULTAR + "')")
    public PageResponse<ClienteResponse> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EstadoGeneral status,
            Pageable pageable) {
        return PageResponse.of(clienteService.listar(search, status, pageable), it -> it);
    }

    @GetMapping("/api/customers/search")
    @PreAuthorize("hasAuthority('" + Permisos.CLIENTES_CONSULTAR + "')")
    public List<ClienteResponse> buscarRapido(@RequestParam String q) {
        return clienteService.buscarRapido(q);
    }

    @GetMapping("/api/customers/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CLIENTES_CONSULTAR + "')")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PostMapping("/api/customers")
    @PreAuthorize("hasAuthority('" + Permisos.CLIENTES_CREAR + "')")
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse creado = clienteService.crear(request);
        return ResponseEntity.created(URI.create("/api/customers/" + creado.id())).body(creado);
    }

    @PutMapping("/api/customers/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CLIENTES_EDITAR + "')")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @PatchMapping("/api/customers/{id}/status")
    @PreAuthorize("hasAuthority('" + Permisos.CLIENTES_EDITAR + "')")
    public ClienteResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return clienteService.cambiarEstado(id, request.status());
    }
}
