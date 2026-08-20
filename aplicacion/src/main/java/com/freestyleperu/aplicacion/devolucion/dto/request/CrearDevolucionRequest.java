package com.freestyleperu.aplicacion.devolucion.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearDevolucionRequest(
        @NotNull Long saleId,
        @NotBlank @Size(max = 255) String reason,
        @NotNull Long refundMethodId,
        @NotEmpty @Valid List<ItemDevolucionRequest> items) {
}
