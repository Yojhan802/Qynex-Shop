package com.freestyleperu.aplicacion.pedido.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarPedidoRequest(@NotBlank @Size(max = 255) String reason) {
}
