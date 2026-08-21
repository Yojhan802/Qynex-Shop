package com.freestyleperu.aplicacion.promotor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromoterRequest(@NotBlank @Size(max = 120) String name) {
}
