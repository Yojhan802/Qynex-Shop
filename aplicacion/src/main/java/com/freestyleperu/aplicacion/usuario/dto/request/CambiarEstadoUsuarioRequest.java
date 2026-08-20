package com.freestyleperu.aplicacion.usuario.dto.request;

import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequest(@NotNull UsuarioEstado status) {
}
