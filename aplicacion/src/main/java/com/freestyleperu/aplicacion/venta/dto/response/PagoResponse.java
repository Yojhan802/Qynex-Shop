package com.freestyleperu.aplicacion.venta.dto.response;

import java.math.BigDecimal;

public record PagoResponse(Long paymentMethodId, String paymentMethodName, BigDecimal amount, String reference) {
}
