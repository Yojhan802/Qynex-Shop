package com.freestyleperu.aplicacion.cliente.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginClienteRequest(@NotBlank String email, @NotBlank String password) {
}
