package com.freestyleperu.aplicacion.pedido.dto.response;

import com.freestyleperu.aplicacion.pedido.domain.PedidoStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoResumenResponse(
        Long id,
        String orderNumber,
        String customerName,
        BigDecimal total,
        PedidoStatus status,
        LocalDateTime createdAt) {
}
