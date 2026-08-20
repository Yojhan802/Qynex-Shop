package com.freestyleperu.aplicacion.producto.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record GenerarVariantesRequest(
        @NotEmpty List<Long> colorIds,
        @NotEmpty List<Long> sizeIds,
        @Min(0) Integer minStock,
        boolean generateBarcodes) {
}
