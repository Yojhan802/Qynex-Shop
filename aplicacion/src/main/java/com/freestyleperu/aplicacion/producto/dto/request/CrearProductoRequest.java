package com.freestyleperu.aplicacion.producto.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CrearProductoRequest(
        @Size(max = 30) String internalCode,
        @Size(max = 40) String sku,
        @NotBlank @Size(max = 150) String name,
        @NotNull Long categoryId,
        Long subcategoryId,
        Long brandId,
        String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @DecimalMin(value = "0.01") BigDecimal promoPrice) {
}
