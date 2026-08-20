package com.freestyleperu.aplicacion.devolucion.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemDevolucionRequest(@NotNull Long saleDetailId, @NotNull @Positive Integer quantity, boolean restock) {
}
