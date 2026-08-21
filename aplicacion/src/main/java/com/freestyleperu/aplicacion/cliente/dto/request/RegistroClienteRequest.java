package com.freestyleperu.aplicacion.cliente.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistroClienteRequest(
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 60)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "debe contener al menos una letra y un número")
        String password,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 20) String phone) {
}
