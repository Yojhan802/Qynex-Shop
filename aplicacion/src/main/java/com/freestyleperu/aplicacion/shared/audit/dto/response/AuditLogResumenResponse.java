package com.freestyleperu.aplicacion.shared.audit.dto.response;

import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import java.time.LocalDateTime;

public record AuditLogResumenResponse(
        Long id,
        Long userId,
        String username,
        String action,
        String entity,
        Long entityId,
        AuditResult result,
        LocalDateTime createdAt) {
}
