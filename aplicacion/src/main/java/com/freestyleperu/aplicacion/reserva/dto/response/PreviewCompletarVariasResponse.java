package com.freestyleperu.aplicacion.reserva.dto.response;

import java.math.BigDecimal;

/**
 * Lo que calzaría al completar un conjunto de separaciones juntas — precio
 * normal, combo detectado automáticamente (si hay, ver RN-28) y saldo
 * pendiente ya restando las señas. Usado por el panel para mostrarle al
 * cajero cuánto cobrar antes de pedirle los montos de pago concretos.
 */
public record PreviewCompletarVariasResponse(
        BigDecimal totalNormal, BigDecimal totalFinal, String comboNombre, BigDecimal totalDeposito, BigDecimal saldoPendiente) {
}
