package com.freestyleperu.aplicacion.reporte.dto.response;

import java.math.BigDecimal;

public record PromotorReporteResponse(String promoterName, long salesCount, BigDecimal total) {
}
