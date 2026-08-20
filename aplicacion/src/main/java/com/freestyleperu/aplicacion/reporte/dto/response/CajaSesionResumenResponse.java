package com.freestyleperu.aplicacion.reporte.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CajaSesionResumenResponse(
        Long sessionId,
        String registerName,
        String openedByUsername,
        String closedByUsername,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal openingAmount,
        BigDecimal expectedAmount,
        BigDecimal countedAmount,
        BigDecimal difference) {
}
