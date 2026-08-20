package com.freestyleperu.aplicacion.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ColorRequest(
        @NotBlank @Size(max = 40) String name,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "debe ser un color HEX válido, p. ej. #1A1A1A") String hexCode) {
}
