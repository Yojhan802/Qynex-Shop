package com.freestyleperu.aplicacion.cliente.dto.response;

import com.freestyleperu.aplicacion.cliente.domain.TipoDocumento;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String fullName,
        TipoDocumento docType,
        String docNumber,
        String phone,
        String email,
        LocalDate birthDate,
        EstadoGeneral status,
        LocalDateTime createdAt) {
}
