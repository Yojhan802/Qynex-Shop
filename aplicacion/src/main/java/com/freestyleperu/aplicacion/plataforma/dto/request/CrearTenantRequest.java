package com.freestyleperu.aplicacion.plataforma.dto.request;

import com.freestyleperu.aplicacion.configuracion.domain.BusinessVertical;
import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CrearTenantRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(min = 3, max = 63)
        @Pattern(regexp = "^[a-z0-9](?:[a-z0-9-]{1,61}[a-z0-9])?$",
                message = "solo admite minúsculas, números y guiones") String slug,
        @Size(max = 15) String ruc,
        @Size(max = 255) String address,
        @Size(max = 20) String phone,
        @Email @Size(max = 120) String email,
        @NotNull BusinessVertical businessVertical,
        @NotNull Plan plan,
        LocalDate nextPaymentDue,
        @NotBlank @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "solo admite letras, números, punto, guion y guion bajo")
        String ownerUsername,
        @Email @Size(max = 120) String ownerEmail,
        @NotBlank @Size(max = 120) String ownerFullName) {
}
