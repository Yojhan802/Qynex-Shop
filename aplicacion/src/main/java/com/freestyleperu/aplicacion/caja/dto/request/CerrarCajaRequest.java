package com.freestyleperu.aplicacion.caja.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CerrarCajaRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal countedAmount,
        @Size(max = 255) String notes) {
}
