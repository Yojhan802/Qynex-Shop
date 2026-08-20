package com.freestyleperu.aplicacion.inventario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SalidaInventarioRequest(
        @NotNull Long variantId,
        Long warehouseId,
        @NotNull @Positive Integer quantity,
        @NotBlank @Size(max = 255) String reason) {
}
