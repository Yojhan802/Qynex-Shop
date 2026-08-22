package com.freestyleperu.aplicacion.reserva.dto.request;

import com.freestyleperu.aplicacion.venta.dto.request.PagoVentaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Completa varias separaciones del mismo comprador en una sola venta —
 * pensado para cuando llega a recoger en persona lo que separó en varios
 * momentos de un live. Si las cantidades seleccionadas calzan con algún
 * combo activo, se aplica automáticamente (RN-28); si no, cada separación
 * se cobra a su propio precio.
 */
public record CompletarVariasReservasRequest(
        @NotEmpty List<Long> reservationIds, @NotNull Long cashSessionId, @NotEmpty @Valid List<PagoVentaRequest> payments) {
}
