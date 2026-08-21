package com.freestyleperu.aplicacion.venta.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CrearVentaRequest(
        Long customerId,
        Long promoterId,
        @NotNull Long cashSessionId,
        @DecimalMin(value = "0.00") BigDecimal discountAmount,
        @Size(max = 255) String notes,
        @NotEmpty @Valid List<ItemVentaRequest> items,
        @NotEmpty @Valid List<PagoVentaRequest> payments) {
}
