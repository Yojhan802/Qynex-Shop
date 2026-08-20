package com.freestyleperu.aplicacion.caja.domain;

/** Solo eventos que realmente mueven el efectivo del cajón (docs/03-modelo-datos.md §9). */
public enum CashMovementType {
    VENTA,
    INGRESO,
    GASTO,
    RETIRO,
    DEVOLUCION
}
