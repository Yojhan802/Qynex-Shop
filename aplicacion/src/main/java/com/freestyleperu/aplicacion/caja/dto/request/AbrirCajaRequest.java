package com.freestyleperu.aplicacion.caja.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AbrirCajaRequest(@NotNull Long cashRegisterId, @NotNull @DecimalMin(value = "0.00") BigDecimal openingAmount) {
}
