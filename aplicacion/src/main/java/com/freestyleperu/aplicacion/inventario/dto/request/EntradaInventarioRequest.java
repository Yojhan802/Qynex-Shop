package com.freestyleperu.aplicacion.inventario.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EntradaInventarioRequest(
        @NotNull Long variantId,
        Long warehouseId,
        @NotNull @Positive Integer quantity,
        @Size(max = 255) String reason) {
}
