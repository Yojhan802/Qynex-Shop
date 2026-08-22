package com.freestyleperu.aplicacion.configuracion.dto.response;

import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code plan}, {@code subscriptionStatus} y {@code nextPaymentDue} son de
 * solo lectura: no viajan en {@code ActualizarCompanySettingsRequest}, el
 * cliente no los puede cambiar (ver RN-23).
 */
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
        BigDecimal reservationDepositAmount,
        int reservationExpirationDays,
        Plan plan,
        SubscriptionStatus subscriptionStatus,
        LocalDate nextPaymentDue,
        LocalDateTime updatedAt,
        String updatedByUsername) {
}
