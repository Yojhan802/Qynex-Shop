package com.freestyleperu.aplicacion.producto.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductoDetalleResponse(
        Long id,
        String internalCode,
        String sku,
        String name,
        Long categoryId,
        String categoryName,
        Long subcategoryId,
        String subcategoryName,
        Long brandId,
        String brandName,
        String description,
        String material,
        String fit,
        BigDecimal price,
        BigDecimal promoPrice,
        EstadoGeneral status,
        String imageUrl,
        String sizeGuideImageUrl,
        List<VarianteResponse> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
