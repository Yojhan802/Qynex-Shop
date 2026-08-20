package com.freestyleperu.aplicacion.reporte.dto.response;

import java.math.BigDecimal;

public record ProductoTopResponse(String productName, long units, BigDecimal total) {
}
