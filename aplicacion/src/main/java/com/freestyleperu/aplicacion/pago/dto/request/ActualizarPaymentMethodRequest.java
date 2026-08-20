package com.freestyleperu.aplicacion.pago.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPaymentMethodRequest(
        @NotBlank @Size(max = 40) String name,
        @Size(max = 150) String accountHolder,
        @Size(max = 30) String accountNumber,
        @Size(max = 255) String qrImageUrl) {
}
