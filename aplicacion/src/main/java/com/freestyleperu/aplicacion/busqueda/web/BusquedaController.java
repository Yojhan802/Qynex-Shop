package com.freestyleperu.aplicacion.busqueda.web;

import com.freestyleperu.aplicacion.busqueda.dto.response.SearchResponse;
import com.freestyleperu.aplicacion.busqueda.service.BusquedaService;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BusquedaController {

    private static final int LONGITUD_MINIMA = 2;

    private final BusquedaService busquedaService;

    public BusquedaController(BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
    }

    /** Sin @PreAuthorize de un solo permiso: cada categoría se filtra dentro del service según lo que el usuario puede ver. */
    @GetMapping("/api/search")
    public SearchResponse buscar(@RequestParam String q, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (q == null || q.trim().length() < LONGITUD_MINIMA) {
            return new SearchResponse(List.of(), List.of(), List.of(), List.of());
        }
        return busquedaService.buscar(q, currentUser);
    }
}
