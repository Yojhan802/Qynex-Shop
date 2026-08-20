package com.freestyleperu.aplicacion.auth.dto;

import java.util.List;

public record UsuarioActualResponse(
        Long id,
        String username,
        String fullName,
        boolean mustChangePassword,
        List<String> roles,
        List<String> permissions) {
}
