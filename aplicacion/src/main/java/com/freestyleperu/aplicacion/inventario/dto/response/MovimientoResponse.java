package com.freestyleperu.aplicacion.inventario.dto.response;

import com.freestyleperu.aplicacion.inventario.domain.MovementType;
import com.freestyleperu.aplicacion.inventario.domain.ReferenceType;
import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long variantId,
        String variantSku,
        String productName,
        String warehouseName,
        MovementType type,
        int quantity,
        int stockBefore,
        int stockAfter,
        ReferenceType referenceType,
        Long referenceId,
        String reason,
        String username,
        LocalDateTime createdAt) {
}
