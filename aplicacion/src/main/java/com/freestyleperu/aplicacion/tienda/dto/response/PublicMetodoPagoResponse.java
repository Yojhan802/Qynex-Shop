package com.freestyleperu.aplicacion.tienda.dto.response;

import com.freestyleperu.aplicacion.pago.domain.PaymentMethodType;

public record PublicMetodoPagoResponse(
        Long id,
        String code,
        String name,
        PaymentMethodType type,
        boolean requiresReference,
        String accountHolder,
        String accountNumber,
        String qrImageUrl) {
}
