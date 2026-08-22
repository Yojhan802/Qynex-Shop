package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code hierarchyLevel} es opcional — si se omite, el rol nace en 0 (no puede asignar ningún rol al crear usuarios salvo otros de nivel 0). Ver RN-25. */
public record CrearRolRequest(
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^[A-Z_]+$", message = "solo mayúsculas y guiones bajos")
        String code,
        @NotBlank @Size(max = 60) String name,
        @Size(max = 255) String description,
        @Min(0) @Max(100) Integer hierarchyLevel) {
}
