package com.freestyleperu.aplicacion.combo.dto.request;

import com.freestyleperu.aplicacion.combo.domain.ComboSelectorType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Exactamente uno de {@code productId} o {@code categoryId} debe venir,
 * según {@code selectorType} — se valida en el servicio. {@code brandId}
 * es siempre opcional y solo tiene efecto cuando {@code selectorType} es
 * {@code CATEGORY} (acota a una marca dentro de esa categoría).
 */
public record ComboItemRequest(
        @NotNull ComboSelectorType selectorType,
        Long productId,
        Long categoryId,
        Long brandId,
        @NotNull @Positive Integer quantity) {
}
