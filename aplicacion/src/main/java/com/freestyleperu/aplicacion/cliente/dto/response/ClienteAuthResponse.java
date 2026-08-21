package com.freestyleperu.aplicacion.cliente.dto.response;

public record ClienteAuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        ClienteActualResponse customer) {
}
