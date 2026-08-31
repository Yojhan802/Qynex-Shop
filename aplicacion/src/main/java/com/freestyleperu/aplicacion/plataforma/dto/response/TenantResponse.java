package com.freestyleperu.aplicacion.plataforma.dto.response;

import com.freestyleperu.aplicacion.configuracion.domain.BusinessVertical;
import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String slug,
        String name,
        String ruc,
        String address,
        String phone,
        String email,
        BusinessVertical businessVertical,
        Plan plan,
        SubscriptionStatus subscriptionStatus,
        LocalDate nextPaymentDue,
        String ownerUsername,
        int activeUsers,
        LocalDateTime updatedAt) {
}
