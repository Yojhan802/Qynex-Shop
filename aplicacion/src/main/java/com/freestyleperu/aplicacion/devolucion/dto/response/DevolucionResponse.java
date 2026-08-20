package com.freestyleperu.aplicacion.devolucion.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DevolucionResponse(
        Long id,
        String returnNumber,
        Long saleId,
        String saleNumber,
        BigDecimal totalAmount,
        String refundMethodName,
        String reason,
        String username,
        LocalDateTime createdAt,
        List<DevolucionItemResponse> items) {
}
