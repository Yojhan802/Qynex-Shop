package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarRolRequest(
        @NotBlank @Size(max = 60) String name,
        @Size(max = 255) String description,
        @Min(0) @Max(100) Integer hierarchyLevel) {
}
