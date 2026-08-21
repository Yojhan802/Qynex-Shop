package com.freestyleperu.aplicacion.tienda.dto.response;

public record PublicVarianteResponse(
        Long variantId,
        Long colorId,
        String colorName,
        String colorHex,
        Long sizeId,
        String sizeName,
        boolean inStock) {
}
