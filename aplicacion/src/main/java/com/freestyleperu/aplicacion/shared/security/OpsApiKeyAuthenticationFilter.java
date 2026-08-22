package com.freestyleperu.aplicacion.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica {@code PUT /api/system/subscription} con una llave secreta propia
 * de esta instalación ({@code OPS_API_KEY}), no con login de usuario — el
 * panel externo de monitoreo (fuera de este sistema) la usa para marcar pagos
 * y suspensiones sin que nadie tenga que entrar a la base de datos a mano.
 * Si {@code OPS_API_KEY} no está configurada, este endpoint queda inalcanzable
 * (fail closed): sin llave, nadie puede autenticarse contra él.
 */
@Component
public class OpsApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Ops-Key";

    private final String opsApiKey;

    public OpsApiKeyAuthenticationFilter(@Value("${app.ops.api-key:}") String opsApiKey) {
        this.opsApiKey = opsApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (!opsApiKey.isBlank() && opsApiKey.equals(header) && SecurityContextHolder.getContext().getAuthentication() == null) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "ops", null, List.of(new SimpleGrantedAuthority("OPS_API")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
