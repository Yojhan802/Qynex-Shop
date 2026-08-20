package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarRolRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 255) String description) {
}
