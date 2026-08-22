package com.freestyleperu.aplicacion.reserva.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * {@code depositAmount} es opcional — si se omite, se usa el monto por
 * defecto de Configuración; es una sola seña para todo el grupo de
 * {@code items}, sin importar cuántas líneas tenga. Exactamente uno de
 * {@code customerId} o {@code guestName} debe venir: un cliente ya
 * registrado, o el nombre de un comprador ocasional (lives de TikTok, etc.)
 * que no necesita registrarse como cliente — ver docs/04-reglas-negocio.md
 * RN-27.
 */
public record CrearReservaRequest(
        Long customerId,
        @Size(max = 150) String guestName,
        @Size(max = 20) String guestPhone,
        @NotEmpty @Valid List<ReservaItemRequest> items,
        @Positive BigDecimal depositAmount,
        @NotNull Long depositPaymentMethodId,
        @Size(max = 50) String depositReference,
        Long promoterId,
        @Size(max = 255) String notes) {
}
