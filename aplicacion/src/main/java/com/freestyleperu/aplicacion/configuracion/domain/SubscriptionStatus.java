package com.freestyleperu.aplicacion.configuracion.domain;

/**
 * Estado de pago de esta instalación — independiente del {@link Plan} (nivel
 * contratado). SUSPENDIDA bloquea todo el sistema hasta que el operador de
 * la plataforma la reactive tras recibir el pago (ver SuscripcionScheduler,
 * SubscriptionStatusFilter, docs/03-modelo-datos.md §15).
 */
public enum SubscriptionStatus {
    ACTIVA,
    SUSPENDIDA
}
