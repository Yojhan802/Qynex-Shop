package com.freestyleperu.aplicacion.configuracion.dto.response;

/**
 * Identidad visual mínima de esta instalación, sin autenticación — para
 * pintar el logo/nombre correctos en pantallas donde todavía no hay sesión
 * (login, "servicio suspendido") y en la tienda pública. Deliberadamente
 * separado de {@link SystemInfoResponse} (pensado para un panel externo de
 * monitoreo, no para el navegador del usuario) y de
 * {@link CompanySettingsResponse} (que trae datos que sí requieren login,
 * como RUC/dirección/contacto).
 */
public record BrandingResponse(String name, String logoUrl) {
}
