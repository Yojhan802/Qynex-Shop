package com.freestyleperu.aplicacion.configuracion.dto.response;

import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** {@code plan} es de solo lectura: no viaja en {@code ActualizarCompanySettingsRequest}, el cliente no lo puede cambiar. */
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
        Plan plan,
        LocalDateTime updatedAt,
        String updatedByUsername) {
}
