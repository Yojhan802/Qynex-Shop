package com.freestyleperu.aplicacion.configuracion.dto.response;

import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import java.time.LocalDate;

/**
 * Ficha pública mínima de esta instalación, sin autenticación — para que un
 * panel central de monitoreo (fuera de este sistema, ver docs/03 §15) pueda
 * preguntarle a cada cliente "¿quién eres, en qué plan estás, qué versión
 * corres, estás al día con el pago?" sin necesitar credenciales.
 * Deliberadamente no incluye nada más (ni RUC, ni datos de contacto) — eso
 * ya vive detrás de login en /api/settings/company. Es de solo lectura por
 * diseño — no existe forma de cambiar subscriptionStatus/nextPaymentDue
 * desde ninguna API, ver RN-23.
 */
public record SystemInfoResponse(
        String name, Plan plan, String version, SubscriptionStatus subscriptionStatus, LocalDate nextPaymentDue) {
}
