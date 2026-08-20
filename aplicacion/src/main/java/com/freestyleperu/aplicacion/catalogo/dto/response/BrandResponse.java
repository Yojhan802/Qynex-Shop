package com.freestyleperu.aplicacion.catalogo.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record BrandResponse(Long id, String name, EstadoGeneral status) {
}
