package com.freestyleperu.aplicacion.caja.dto.response;

import java.math.BigDecimal;

public record ResumenCierreResponse(BigDecimal openingAmount, BigDecimal movementsTotal, BigDecimal expectedAmount) {
}
