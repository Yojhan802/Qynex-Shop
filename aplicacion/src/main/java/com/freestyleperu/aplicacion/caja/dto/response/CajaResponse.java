package com.freestyleperu.aplicacion.caja.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record CajaResponse(Long id, String branchName, String code, String name, EstadoGeneral status) {
}
