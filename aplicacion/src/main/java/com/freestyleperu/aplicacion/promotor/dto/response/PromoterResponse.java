package com.freestyleperu.aplicacion.promotor.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record PromoterResponse(Long id, String name, EstadoGeneral status) {
}
