package com.freestyleperu.aplicacion.configuracion.domain;

/**
 * Plan de suscripción de esta instalación (modelo SaaS: un despliegue por
 * cliente). El orden declarado importa — cada plan incluye todo lo del
 * anterior, así que se compara por ordinal (ver {@code PlanGate}).
 */
public enum Plan {
    STARTER,
    PROFESIONAL,
    ECOMMERCE,
    IA
}
