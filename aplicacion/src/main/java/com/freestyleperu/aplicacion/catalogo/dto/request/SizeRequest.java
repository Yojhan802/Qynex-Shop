package com.freestyleperu.aplicacion.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SizeRequest(@NotBlank @Size(max = 20) String name, @NotNull Short sortOrder) {
}
