package com.freestyleperu.aplicacion.venta.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * El precio unitario nunca viaja en la petición: el backend siempre lo
 * resuelve desde el precio vigente del producto (docs/01-requisitos.md §42,
 * "el backend siempre debe considerarse la autoridad final").
 */
public record ItemVentaRequest(
        @NotNull Long variantId,
        @NotNull @Positive Integer quantity,
        @DecimalMin(value = "0.00") BigDecimal discountAmount) {
}
