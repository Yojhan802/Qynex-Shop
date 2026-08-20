package com.freestyleperu.aplicacion.catalogo.dto.response;

import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record SubcategoryResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String name,
        String slug,
        EstadoGeneral status) {
}
