package com.freestyleperu.aplicacion.usuario.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AsignarPermisosRequest(@NotNull List<Long> permissionIds) {
}
