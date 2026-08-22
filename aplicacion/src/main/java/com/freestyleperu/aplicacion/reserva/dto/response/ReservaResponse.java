package com.freestyleperu.aplicacion.reserva.dto.response;

import com.freestyleperu.aplicacion.reserva.domain.ReservaStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReservaResponse(
        Long id,
        String reservationNumber,
        Long customerId,
        String customerName,
        boolean guest,
        String guestPhone,
        List<ReservaItemResponse> items,
        BigDecimal total,
        BigDecimal depositAmount,
        String depositPaymentMethodName,
        String depositReference,
        Long promoterId,
        String promoterName,
        ReservaStatus status,
        LocalDateTime expiresAt,
        String notes,
        String createdByUsername,
        LocalDateTime createdAt,
        Long saleId,
        LocalDateTime completedAt,
        String completedByUsername,
        LocalDateTime cancelledAt,
        String cancelledByUsername,
        String cancellationReason) {
}
