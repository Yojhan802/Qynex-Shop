package com.freestyleperu.aplicacion.reclamo.dto.response;

import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Constancia de la hoja de reclamación tal como debe entregarse al consumidor
 * (D.S. 011-2011-PCM, Art. 5): copia íntegra de lo que declaró, más los datos
 * del proveedor y el plazo máximo de respuesta.
 *
 * <p>Solo se devuelve como respuesta inmediata al registro, nunca por número de
 * hoja: los correlativos son predecibles y consultarlos libremente expondría
 * datos personales de otros consumidores. La consulta posterior por código usa
 * {@link PublicComplaintResponse}, que no incluye datos personales.
 */
public record ComplaintReceiptResponse(
        String entryNumber,
        ComplaintType type,
        ComplaintStatus status,
        String providerName,
        String providerRuc,
        String providerAddress,
        String consumerName,
        String consumerDocument,
        String consumerEmail,
        String consumerPhone,
        String consumerAddress,
        String orderNumber,
        String productServiceDescription,
        BigDecimal amount,
        String detail,
        String consumerRequest,
        LocalDateTime createdAt,
        LocalDate responseDueDate,
        /**
         * Permiso de corta vida para descargar esta constancia en PDF. Solo viaja aquí, en la
         * respuesta al registro: es lo que permite ofrecer la descarga sin abrir un endpoint
         * por número de hoja, que sería enumerable.
         */
        String receiptToken) {
}
