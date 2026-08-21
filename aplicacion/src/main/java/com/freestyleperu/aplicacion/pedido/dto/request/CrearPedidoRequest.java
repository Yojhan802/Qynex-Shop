package com.freestyleperu.aplicacion.pedido.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearPedidoRequest(
        @NotEmpty @Valid List<ItemPedidoRequest> items,
        @NotNull Long paymentMethodId,
        @Size(max = 50) String paymentReference,
        @NotBlank @Size(max = 15) String recipientDni,
        @NotBlank @Size(max = 100) String recipientFirstName,
        @NotBlank @Size(max = 60) String recipientLastNamePaterno,
        @NotBlank @Size(max = 60) String recipientLastNameMaterno,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 255) String address,
        @NotBlank @Size(max = 100) String department,
        @NotBlank @Size(max = 100) String province,
        @NotBlank @Size(max = 100) String district,
        @Size(max = 255) String notes) {
}
