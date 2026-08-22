package com.freestyleperu.aplicacion.reserva.dto.response;

import com.freestyleperu.aplicacion.reserva.domain.ReservaStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code itemsSummary} lista los productos apartados (ej. "Polo Negro M ×4, Casaca Azul L ×1") para el listado. */
public record ReservaResumenResponse(
        Long id,
        String reservationNumber,
        String customerName,
        boolean guest,
        String itemsSummary,
        int totalQuantity,
        BigDecimal total,
        BigDecimal depositAmount,
        ReservaStatus status,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {
}
