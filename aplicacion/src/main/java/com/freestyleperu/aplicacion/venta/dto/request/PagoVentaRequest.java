package com.freestyleperu.aplicacion.venta.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PagoVentaRequest(
        @NotNull Long paymentMethodId,
        @NotNull @Positive BigDecimal amount,
        @Size(max = 50) String reference) {
}
