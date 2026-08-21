package com.freestyleperu.aplicacion.shared.audit;

import com.freestyleperu.aplicacion.shared.audit.dto.response.AuditLogResponse;
import com.freestyleperu.aplicacion.shared.audit.dto.response.AuditLogResumenResponse;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lado de consulta de auditoría — separado de {@link AuditService}, que solo escribe. */
@Service
@Transactional(readOnly = true)
public class AuditoriaService {

    private final AuditLogRepository auditLogRepository;

    public AuditoriaService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PageResponse<AuditLogResumenResponse> listar(
            Long userId, String action, String entity, AuditResult result, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return PageResponse.of(
                auditLogRepository.buscar(userId, action, entity, result, from, to, pageable), this::toResumen);
    }

    public AuditLogResponse obtener(Long id) {
        AuditLog log = auditLogRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Registro de auditoría", id));
        return toResponse(log);
    }

    private AuditLogResumenResponse toResumen(AuditLog log) {
        return new AuditLogResumenResponse(
                log.getId(), log.getUserId(), log.getUsername(), log.getAction(), log.getEntity(), log.getEntityId(),
                log.getResult(), log.getCreatedAt());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(), log.getUserId(), log.getUsername(), log.getAction(), log.getEntity(), log.getEntityId(),
                log.getOldValue(), log.getNewValue(), log.getResult(), log.getIpAddress(), log.getUserAgent(), log.getCreatedAt());
    }
}
