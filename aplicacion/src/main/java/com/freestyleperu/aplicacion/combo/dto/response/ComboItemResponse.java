package com.freestyleperu.aplicacion.combo.dto.response;

import com.freestyleperu.aplicacion.combo.domain.ComboSelectorType;

public record ComboItemResponse(
        ComboSelectorType selectorType,
        Long productId,
        String productName,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        int quantity) {
}
