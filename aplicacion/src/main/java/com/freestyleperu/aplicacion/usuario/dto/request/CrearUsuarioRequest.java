package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CrearUsuarioRequest(
        @NotBlank @Size(min = 4, max = 50) String username,
        @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 60)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener al menos una letra y un número")
        String password,
        @NotBlank @Size(max = 120) String fullName,
        @Size(max = 15) String dni,
        @Size(max = 20) String phone,
        @NotEmpty List<Long> roleIds) {
}
