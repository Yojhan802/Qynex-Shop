package com.freestyleperu.aplicacion.reserva.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarReservaRequest(@NotBlank @Size(max = 255) String reason) {
}
