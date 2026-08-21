package com.freestyleperu.aplicacion.tienda.dto.response;

import java.math.BigDecimal;

public record PublicProductoResumenResponse(
        Long id,
        String name,
        BigDecimal price,
        BigDecimal promoPrice,
        String imageUrl,
        String categoryName,
        String brandName,
        boolean inStock) {
}
