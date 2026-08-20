package com.freestyleperu.aplicacion.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, int expiresIn) {
}
