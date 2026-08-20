package com.freestyleperu.aplicacion.shared.dto;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoRequest(@NotNull EstadoGeneral status) {
}
