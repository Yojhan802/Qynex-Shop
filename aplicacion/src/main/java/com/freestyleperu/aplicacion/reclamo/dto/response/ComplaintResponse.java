package com.freestyleperu.aplicacion.reclamo.dto.response;

import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComplaintResponse(
        Long id,
        String entryNumber,
        ComplaintType type,
        ComplaintStatus status,
        String providerName,
        String providerRuc,
        String providerAddress,
        String consumerName,
        String consumerDocument,
        String consumerEmail,
        String consumerPhone,
        String consumerAddress,
        String orderNumber,
        String saleNumber,
        String productServiceDescription,
        BigDecimal amount,
        String detail,
        String consumerRequest,
        String response,
        LocalDateTime createdAt,
        LocalDateTime respondedAt,
        /**
         * Fecha límite para responder al consumidor (D.S. 011-2011-PCM: 30 días calendario
         * desde el registro). Se envía calculada y no se deja al frontend: es un plazo legal,
         * y que cada pantalla lo recalcule por su cuenta es como se acaban desincronizando.
         */
        LocalDate responseDueDate) {
}
