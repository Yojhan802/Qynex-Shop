package com.freestyleperu.aplicacion.venta.dto.response;

import com.freestyleperu.aplicacion.venta.domain.SaleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VentaResumenResponse(
        Long id,
        String saleNumber,
        String customerName,
        String sellerName,
        BigDecimal total,
        SaleStatus status,
        LocalDateTime createdAt) {
}
