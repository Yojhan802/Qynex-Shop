package com.freestyleperu.aplicacion.caja.dto.response;

import com.freestyleperu.aplicacion.caja.domain.CashSessionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SesionCajaDetalleResponse(
        Long id,
        Long cashRegisterId,
        String cashRegisterName,
        String openedByUsername,
        BigDecimal openingAmount,
        LocalDateTime openedAt,
        BigDecimal expectedAmount,
        BigDecimal countedAmount,
        BigDecimal difference,
        String closedByUsername,
        LocalDateTime closedAt,
        CashSessionStatus status,
        String notes,
        List<MovimientoCajaResponse> movements) {
}
