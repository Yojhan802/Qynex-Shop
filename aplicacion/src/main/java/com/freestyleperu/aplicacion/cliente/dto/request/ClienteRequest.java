package com.freestyleperu.aplicacion.cliente.dto.request;

import com.freestyleperu.aplicacion.cliente.domain.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotNull TipoDocumento docType,
        @Size(max = 15) String docNumber,
        @Size(max = 20) String phone,
        @Email @Size(max = 120) String email,
        LocalDate birthDate) {
}
