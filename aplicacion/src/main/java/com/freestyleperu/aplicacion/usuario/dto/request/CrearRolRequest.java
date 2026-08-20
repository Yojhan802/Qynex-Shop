package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearRolRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^[A-Z_]+$", message = "solo mayúsculas y guiones bajos")
        String code,
        @NotBlank @Size(max = 60) String name,
        @Size(max = 255) String description) {
}
