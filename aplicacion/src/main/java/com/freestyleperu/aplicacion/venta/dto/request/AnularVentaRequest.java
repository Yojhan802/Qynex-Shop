package com.freestyleperu.aplicacion.venta.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularVentaRequest(@NotBlank @Size(max = 255) String reason) {
}
