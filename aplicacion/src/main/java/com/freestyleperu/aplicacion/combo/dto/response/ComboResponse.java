package com.freestyleperu.aplicacion.combo.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.math.BigDecimal;
import java.util.List;

public record ComboResponse(
        Long id,
        String code,
        String name,
        BigDecimal price,
        BigDecimal normalTotal,
        BigDecimal savings,
        EstadoGeneral status,
        List<ComboItemResponse> items) {
}
