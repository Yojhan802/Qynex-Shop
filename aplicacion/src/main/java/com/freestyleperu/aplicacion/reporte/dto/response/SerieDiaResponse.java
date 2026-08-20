package com.freestyleperu.aplicacion.reporte.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SerieDiaResponse(LocalDate date, BigDecimal total) {
}
