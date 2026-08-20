package com.freestyleperu.aplicacion.reporte.dto.response;

import java.math.BigDecimal;

/** Serie genérica etiqueta→total, usada para categoría, vendedor y método de pago. */
public record SerieEtiquetaResponse(String label, BigDecimal total, Double percentage) {
}
