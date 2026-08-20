package com.freestyleperu.aplicacion.producto.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResumenResponse(
        Long id,
        String internalCode,
        String sku,
        String name,
        String categoryName,
        String brandName,
        BigDecimal price,
        BigDecimal promoPrice,
        EstadoGeneral status,
        String imageUrl,
        int variantCount,
        int totalStock,
        LocalDateTime createdAt) {
}
