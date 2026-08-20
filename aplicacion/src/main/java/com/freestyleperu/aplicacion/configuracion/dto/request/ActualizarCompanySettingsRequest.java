package com.freestyleperu.aplicacion.configuracion.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActualizarCompanySettingsRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 15) String ruc,
        @Size(max = 255) String address,
        @Size(max = 20) String phone,
        @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 3) String currencyCode,
        @NotBlank @Size(max = 5) String currencySymbol,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal igvRate,
        @Size(max = 255) String ticketFooter) {
}
