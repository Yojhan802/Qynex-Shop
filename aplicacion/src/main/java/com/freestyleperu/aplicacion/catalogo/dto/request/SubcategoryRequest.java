package com.freestyleperu.aplicacion.catalogo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubcategoryRequest(@NotNull Long categoryId, @NotBlank @Size(max = 80) String name) {
}
