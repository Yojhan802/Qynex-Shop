package com.freestyleperu.aplicacion.inventario.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AjusteInventarioRequest(
        @NotNull Long variantId,
        Long warehouseId,
        @NotNull @Min(0) Integer newStock,
        @NotBlank @Size(max = 255) String reason) {
}
