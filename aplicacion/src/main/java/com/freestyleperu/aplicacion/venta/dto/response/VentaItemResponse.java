package com.freestyleperu.aplicacion.venta.dto.response;

import java.math.BigDecimal;

public record VentaItemResponse(
        Long variantId,
        String productName,
        String variantSku,
        String colorName,
        String sizeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal subtotal,
        Long comboId,
        Long promotionId) {
}
