package com.freestyleperu.aplicacion.pedido.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemPedidoRequest(@NotNull Long variantId, @NotNull @Positive Integer quantity) {
}
