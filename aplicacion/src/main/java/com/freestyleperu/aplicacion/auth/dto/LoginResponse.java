package com.freestyleperu.aplicacion.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        UsuarioActualResponse user) {
}
