package com.freestyleperu.aplicacion.configuracion;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import java.time.LocalDate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revisión diaria del vencimiento de pago de esta instalación (SaaS de un
 * despliegue por cliente, ver docs/03-modelo-datos.md §15). Si se pasa
 * {@code nextPaymentDue} más el margen de gracia, se suspende sola —
 * SubscriptionStatusFilter bloquea entonces todo el sistema hasta que el
 * operador de la plataforma la reactive tras recibir el pago.
 */
@Component
public class SuscripcionScheduler {

    private static final long SETTINGS_ID = 1L;
    private static final int DIAS_GRACIA = 5;

    private final CompanySettingsRepository companySettingsRepository;
    private final AuditService auditService;

    public SuscripcionScheduler(CompanySettingsRepository companySettingsRepository, AuditService auditService) {
        this.companySettingsRepository = companySettingsRepository;
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void revisarVencimiento() {
        companySettingsRepository.findById(SETTINGS_ID).ifPresent(settings -> {
            if (debeSuspenderse(settings)) {
                settings.setSubscriptionStatus(SubscriptionStatus.SUSPENDIDA);
                auditService.log("SUSCRIPCION_SUSPENDIDA_AUTOMATICAMENTE", "COMPANY_SETTINGS", SETTINGS_ID,
                        null, settings.getNextPaymentDue(), AuditResult.SUCCESS);
            }
        });
    }

    private boolean debeSuspenderse(CompanySettings settings) {
        return settings.getSubscriptionStatus() == SubscriptionStatus.ACTIVA
                && settings.getNextPaymentDue() != null
                && LocalDate.now().isAfter(settings.getNextPaymentDue().plusDays(DIAS_GRACIA));
    }
}
