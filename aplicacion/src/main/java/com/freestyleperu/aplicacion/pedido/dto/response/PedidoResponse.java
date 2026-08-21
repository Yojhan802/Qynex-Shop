package com.freestyleperu.aplicacion.pedido.dto.response;

import com.freestyleperu.aplicacion.pedido.domain.PedidoStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal total,
        PedidoStatus status,
        Long paymentMethodId,
        String paymentMethodName,
        String paymentReference,
        String paymentProofUrl,
        String recipientDni,
        String recipientFirstName,
        String recipientLastNamePaterno,
        String recipientLastNameMaterno,
        String phone,
        String address,
        String department,
        String province,
        String district,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        String confirmedByUsername,
        LocalDateTime cancelledAt,
        String cancellationReason,
        List<PedidoItemResponse> items) {
}
