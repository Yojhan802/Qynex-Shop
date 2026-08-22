package com.freestyleperu.aplicacion.promocion.dto.response;

import com.freestyleperu.aplicacion.promocion.domain.PromotionScope;
import com.freestyleperu.aplicacion.promocion.domain.PromotionType;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromocionResponse(
        Long id,
        String code,
        String name,
        PromotionType discountType,
        BigDecimal discountValue,
        PromotionScope scopeType,
        Long scopeCategoryId,
        String scopeCategoryName,
        Long scopeProductId,
        String scopeProductName,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        EstadoGeneral status,
        boolean visibleOnline) {
}
