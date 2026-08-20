package com.freestyleperu.aplicacion.devolucion.dto.response;

import java.math.BigDecimal;

public record DevolucionItemResponse(
        Long saleDetailId,
        Long variantId,
        String productName,
        String variantSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        boolean restock) {
}
