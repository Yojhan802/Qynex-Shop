package com.freestyleperu.aplicacion.reserva.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Una línea de producto dentro de una separación. Cuando viene de aplicar un
 * combo (botón "+ Agregar combo" en el panel), {@code comboId} viene lleno y
 * {@code comboGroup} identifica cuál aplicación del combo es (0, 1, 2...) —
 * necesario para distinguir dos aplicaciones del mismo combo en una misma
 * separación (ej. "8 polos" = 2 aplicaciones de un combo de 4 unidades).
 */
public record ReservaItemRequest(
        @NotNull Long variantId, @NotNull @Positive Integer quantity, Long comboId, Integer comboGroup) {
}
