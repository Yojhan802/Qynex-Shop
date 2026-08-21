package com.freestyleperu.aplicacion.configuracion.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanySettingsResponse(
        String name,
        String ruc,
        String address,
        String phone,
        String email,
        String logoUrl,
        String currencyCode,
        String currencySymbol,
        BigDecimal igvRate,
        String ticketFooter,
        BigDecimal shippingFlatRate,
        LocalDateTime updatedAt,
        String updatedByUsername) {
}
