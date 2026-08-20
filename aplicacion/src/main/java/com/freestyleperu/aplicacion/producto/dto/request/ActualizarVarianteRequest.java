package com.freestyleperu.aplicacion.producto.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ActualizarVarianteRequest(@NotNull @Min(0) Integer minStock) {
}
