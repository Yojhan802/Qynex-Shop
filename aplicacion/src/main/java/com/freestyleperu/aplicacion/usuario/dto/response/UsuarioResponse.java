package com.freestyleperu.aplicacion.usuario.dto.response;

import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import java.time.LocalDateTime;
import java.util.List;

public record UsuarioResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String dni,
        String phone,
        UsuarioEstado status,
        boolean mustChangePassword,
        LocalDateTime lastLoginAt,
        List<RolResumenResponse> roles,
        LocalDateTime createdAt) {
}
