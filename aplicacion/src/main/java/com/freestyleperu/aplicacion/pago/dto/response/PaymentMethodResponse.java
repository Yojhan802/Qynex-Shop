package com.freestyleperu.aplicacion.pago.dto.response;

import com.freestyleperu.aplicacion.pago.domain.PaymentMethodType;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;

public record PaymentMethodResponse(
        Long id,
        String code,
        String name,
        PaymentMethodType type,
        boolean affectsCash,
        boolean requiresReference,
        String accountHolder,
        String accountNumber,
        String qrImageUrl,
        EstadoGeneral status,
        short sortOrder) {
}
