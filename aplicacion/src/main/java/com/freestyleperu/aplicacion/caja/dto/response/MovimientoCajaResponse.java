package com.freestyleperu.aplicacion.caja.dto.response;

import com.freestyleperu.aplicacion.caja.domain.CashMovementType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoCajaResponse(
        Long id,
        Long cashSessionId,
        CashMovementType type,
        BigDecimal amount,
        String reason,
        String username,
        LocalDateTime createdAt) {
}
