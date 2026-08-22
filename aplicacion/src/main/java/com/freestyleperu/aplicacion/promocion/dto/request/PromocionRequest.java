package com.freestyleperu.aplicacion.promocion.dto.request;

import com.freestyleperu.aplicacion.promocion.domain.PromotionScope;
import com.freestyleperu.aplicacion.promocion.domain.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * {@code scopeCategoryId}/{@code scopeProductId} solo aplican según
 * {@code scopeType} (CATEGORY/PRODUCT respectivamente) — se validan en el
 * servicio. {@code startsAt}/{@code endsAt} son opcionales: nulo = sin límite
 * por ese lado.
 */
public record PromocionRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull PromotionType discountType,
        @NotNull @DecimalMin(value = "0.01") BigDecimal discountValue,
        @NotNull PromotionScope scopeType,
        Long scopeCategoryId,
        Long scopeProductId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean visibleOnline) {
}
