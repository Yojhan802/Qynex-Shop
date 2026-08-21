package com.freestyleperu.aplicacion.configuracion.dto.response;

import com.freestyleperu.aplicacion.configuracion.domain.Plan;

/**
 * Ficha pública mínima de esta instalación, sin autenticación — para que un
 * panel central de monitoreo (fuera de este sistema, ver docs/03 §15) pueda
 * preguntarle a cada cliente "¿quién eres, en qué plan estás, qué versión
 * corres?" sin necesitar credenciales. Deliberadamente no incluye nada más
 * (ni RUC, ni datos de contacto) — eso ya vive detrás de login en
 * /api/settings/company.
 */
public record SystemInfoResponse(String name, Plan plan, String version) {
}
