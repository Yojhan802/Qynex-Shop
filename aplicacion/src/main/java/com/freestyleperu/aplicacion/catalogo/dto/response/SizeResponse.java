package com.freestyleperu.aplicacion.catalogo.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record SizeResponse(Long id, String name, short sortOrder, EstadoGeneral status) {
}
