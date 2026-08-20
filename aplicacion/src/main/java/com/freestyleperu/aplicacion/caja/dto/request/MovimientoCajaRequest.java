package com.freestyleperu.aplicacion.caja.dto.request;

import com.freestyleperu.aplicacion.caja.domain.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MovimientoCajaRequest(
        @NotNull CashMovementType type,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(max = 255) String reason) {
}
