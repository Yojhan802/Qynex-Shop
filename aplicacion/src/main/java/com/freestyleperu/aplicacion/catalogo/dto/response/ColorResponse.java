package com.freestyleperu.aplicacion.catalogo.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record ColorResponse(Long id, String name, String hexCode, EstadoGeneral status) {
}
