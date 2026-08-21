package com.freestyleperu.aplicacion.shared.audit.dto.response;

import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long userId,
        String username,
        String action,
        String entity,
        Long entityId,
        String oldValue,
        String newValue,
        AuditResult result,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt) {
}
