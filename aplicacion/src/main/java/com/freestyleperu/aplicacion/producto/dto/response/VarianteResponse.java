package com.freestyleperu.aplicacion.producto.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record VarianteResponse(
        Long id,
        Long productId,
        String productName,
        Long colorId,
        String colorName,
        String colorHex,
        Long sizeId,
        String sizeName,
        String sku,
        String barcode,
        int stock,
        int minStock,
        EstadoGeneral status) {
}
