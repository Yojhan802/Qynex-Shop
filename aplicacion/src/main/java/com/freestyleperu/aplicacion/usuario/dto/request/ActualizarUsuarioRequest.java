package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ActualizarUsuarioRequest(
        @Email @Size(max = 120) String email,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 15) String dni,
        @Size(max = 20) String phone,
        @NotEmpty List<Long> roleIds) {
}
