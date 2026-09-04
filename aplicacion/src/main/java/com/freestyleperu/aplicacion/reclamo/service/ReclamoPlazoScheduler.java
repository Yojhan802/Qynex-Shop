package com.freestyleperu.aplicacion.reclamo.service;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintBookEntry;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import com.freestyleperu.aplicacion.reclamo.repository.ComplaintBookEntryRepository;
import com.freestyleperu.aplicacion.shared.correo.CorreoService;
import com.freestyleperu.aplicacion.shared.security.TenantContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Aviso a cada empresa de las hojas de reclamación cuyo plazo legal de respuesta se acerca
 * o ya venció (D.S. 011-2011-PCM: 30 días calendario desde el registro).
 *
 * <p>El panel ya muestra el plazo, pero solo lo ve quien abre esa pantalla. Si nadie entra
 * en dos semanas el plazo se pasa igual, y pasarlo es sancionable: este job va a buscar a la
 * empresa en vez de esperar a que mire.
 *
 * <p>Como {@code ComplaintBookEntry} lleva {@code @TenantId}, sus consultas se filtran por el
 * tenant activo. Un job no corre dentro de una petición, así que nadie fija ese contexto: hay
 * que hacerlo explícitamente por empresa, igual que en {@code SuscripcionScheduler}.
 */
@Component
public class ReclamoPlazoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReclamoPlazoScheduler.class);
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Se avisa a partir de aquí. Cinco días dejan margen real para investigar y contestar. */
    private static final int DIAS_DE_AVISO = 5;

    /**
     * Días entre recordatorios de la misma hoja. Sin esto, una hoja pendiente generaría un
     * correo diario; con esto insiste, pero sin volverse ruido que se acabe ignorando.
     */
    private static final int DIAS_ENTRE_AVISOS = 7;

    private final CompanySettingsRepository companySettingsRepository;
    private final ComplaintBookEntryRepository complaintRepository;
    private final CorreoService correoService;
    private final TransactionTemplate transacciones;
    private final int plazoRespuestaDias;

    public ReclamoPlazoScheduler(CompanySettingsRepository companySettingsRepository,
            ComplaintBookEntryRepository complaintRepository, CorreoService correoService,
            PlatformTransactionManager transactionManager) {
        this.companySettingsRepository = companySettingsRepository;
        this.complaintRepository = complaintRepository;
        this.correoService = correoService;
        this.transacciones = new TransactionTemplate(transactionManager);
        this.plazoRespuestaDias = ComplaintBookService.PLAZO_RESPUESTA_DIAS;
    }

    /**
     * Una transacción por empresa, y no una que las envuelva a todas. Las hojas llevan
     * {@code @TenantId}, así que Hibernate añade el tenant al WHERE del UPDATE: si el flush
     * ocurriera al final, con el contexto ya cambiado o limpio, la actualización no casaría
     * ninguna fila y las hojas se darían por avisadas sin haberse marcado. Commitear dentro
     * del contexto de cada empresa lo evita, y de paso un fallo no arrastra a las demás.
     */
    @Scheduled(cron = "0 30 8 * * *")
    public void avisarPlazosPorVencer() {
        if (!correoService.configurado()) {
            log.debug("Correo no configurado: no se revisan plazos de reclamos");
            return;
        }
        for (CompanySettings empresa : companySettingsRepository.findAll()) {
            TenantContext.set(empresa.getId());
            try {
                transacciones.executeWithoutResult(estado -> avisarA(empresa));
            } catch (RuntimeException ex) {
                // Que una empresa falle no puede dejar sin aviso a las demás.
                log.error("No se pudo revisar los plazos de reclamos de la empresa {}: {}",
                        empresa.getId(), ex.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void avisarA(CompanySettings empresa) {
        LocalDate hoy = LocalDate.now();
        List<ComplaintBookEntry> urgentes = complaintRepository
                .findAllByStatusOrderByCreatedAtAsc(ComplaintStatus.PENDIENTE).stream()
                .filter(hoja -> !vence(hoja).isAfter(hoy.plusDays(DIAS_DE_AVISO)))
                .filter(hoja -> tocaAvisar(hoja, hoy))
                .toList();
        if (urgentes.isEmpty()) return;

        if (empresa.getEmail() == null || empresa.getEmail().isBlank()) {
            log.warn("La empresa {} tiene {} hoja(s) con el plazo encima y no tiene correo configurado",
                    empresa.getId(), urgentes.size());
            return;
        }
        if (correoService.enviar(empresa.getEmail(), asunto(urgentes.size()), cuerpo(empresa, urgentes, hoy))) {
            // Solo se marcan si el correo salió: si falla, se reintenta mañana en vez de
            // dar por avisada a una empresa que nunca recibió nada.
            urgentes.forEach(hoja -> hoja.setDeadlineReminderAt(LocalDateTime.now()));
        }
    }

    private LocalDate vence(ComplaintBookEntry hoja) {
        return hoja.getCreatedAt().toLocalDate().plusDays(plazoRespuestaDias);
    }

    private boolean tocaAvisar(ComplaintBookEntry hoja, LocalDate hoy) {
        return hoja.getDeadlineReminderAt() == null
                || !hoja.getDeadlineReminderAt().toLocalDate().isAfter(hoy.minusDays(DIAS_ENTRE_AVISOS));
    }

    private String asunto(int cuantas) {
        return cuantas == 1
                ? "Tienes 1 reclamo por responder dentro del plazo legal"
                : "Tienes " + cuantas + " reclamos por responder dentro del plazo legal";
    }

    private String cuerpo(CompanySettings empresa, List<ComplaintBookEntry> urgentes, LocalDate hoy) {
        StringBuilder texto = new StringBuilder();
        texto.append(empresa.getName()).append(":\n\n")
                .append("Estas hojas de tu Libro de Reclamaciones siguen sin respuesta y su plazo ")
                .append("legal está por vencer o ya venció:\n\n");
        for (ComplaintBookEntry hoja : urgentes) {
            long dias = hoy.until(vence(hoja)).getDays();
            texto.append("  · ").append(hoja.getEntryNumber())
                    .append("  ").append(hoja.getConsumerName())
                    .append("  vence el ").append(vence(hoja).format(DIA))
                    .append(dias < 0 ? "  (VENCIDO hace " + Math.abs(dias) + " días)"
                            : dias == 0 ? "  (VENCE HOY)" : "  (quedan " + dias + " días)")
                    .append("\n");
        }
        texto.append("\nRespóndelas desde el panel, en Libro de Reclamaciones.\n")
                .append("\n---\n")
                .append("El D.S. 011-2011-PCM da ").append(plazoRespuestaDias)
                .append(" días calendario para responder al consumidor. No hacerlo dentro del ")
                .append("plazo es una infracción sancionable por INDECOPI.\n");
        return texto.toString();
    }
}
