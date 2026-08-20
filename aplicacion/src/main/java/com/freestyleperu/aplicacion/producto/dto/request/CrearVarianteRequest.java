package com.freestyleperu.aplicacion.producto.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearVarianteRequest(
        @NotNull Long colorId,
        @NotNull Long sizeId,
        String sku,
        String barcode,
        @Min(0) Integer stock,
        @Min(0) Integer minStock,
        boolean generateBarcode) {
}
