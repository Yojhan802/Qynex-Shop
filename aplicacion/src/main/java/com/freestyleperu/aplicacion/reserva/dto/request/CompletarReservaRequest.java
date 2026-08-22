package com.freestyleperu.aplicacion.reserva.dto.request;

import com.freestyleperu.aplicacion.venta.dto.request.PagoVentaRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** {@code payments} debe sumar exactamente el saldo pendiente (precio total menos la seña ya pagada). */
public record CompletarReservaRequest(@NotNull Long cashSessionId, @NotEmpty @Valid List<PagoVentaRequest> payments) {
}
