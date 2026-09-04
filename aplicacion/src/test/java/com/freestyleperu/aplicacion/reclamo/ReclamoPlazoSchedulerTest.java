package com.freestyleperu.aplicacion.reclamo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintBookEntry;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import com.freestyleperu.aplicacion.reclamo.repository.ComplaintBookEntryRepository;
import com.freestyleperu.aplicacion.reclamo.service.ReclamoPlazoScheduler;
import com.freestyleperu.aplicacion.shared.correo.CorreoService;
import com.freestyleperu.aplicacion.shared.security.TenantContext;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * El plazo del D.S. 011-2011-PCM corre aunque nadie abra el panel. Este job es lo que
 * convierte el dato en un aviso, así que lo que importa es a quién avisa, cuándo deja de
 * insistir, y que no dé por avisado a quien no recibió nada.
 */
class ReclamoPlazoSchedulerTest {

    @AfterEach
    void limpiarTenant() {
        TenantContext.clear();
    }

    @Test
    void avisaSoloDeLasHojasConElPlazoEncimaYLasMarca() {
        ComplaintBookEntry urgente = hoja("RC-1", "Ana Ruiz", 27);   // vence en 3 días
        ComplaintBookEntry vencida = hoja("RC-2", "Luis Paz", 34);   // vencida hace 4
        ComplaintBookEntry holgada = hoja("RC-3", "Eva Sosa", 2);    // le quedan 28

        Escenario esc = escenario(List.of(urgente, vencida, holgada), true);
        esc.scheduler().avisarPlazosPorVencer();

        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(esc.correo()).enviar(anyString(), anyString(), cuerpo.capture());
        assertThat(cuerpo.getValue()).contains("RC-1", "RC-2").doesNotContain("RC-3");
        // La vencida se nombra como tal: no es lo mismo que una a punto de vencer.
        assertThat(cuerpo.getValue()).contains("VENCIDO hace 4");

        assertThat(urgente.getDeadlineReminderAt()).isNotNull();
        assertThat(vencida.getDeadlineReminderAt()).isNotNull();
        assertThat(holgada.getDeadlineReminderAt()).isNull();
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void noMarcaComoAvisadaUnaHojaCuyoCorreoNoSalio() {
        ComplaintBookEntry urgente = hoja("RC-9", "Ana Ruiz", 28);
        Escenario esc = escenario(List.of(urgente), false);

        esc.scheduler().avisarPlazosPorVencer();

        // Si se marcara, el fallo de hoy dejaría a la empresa sin aviso para siempre.
        assertThat(urgente.getDeadlineReminderAt()).isNull();
    }

    @Test
    void noRepiteElAvisoAlDiaSiguientePeroSiPasadaLaSemana() {
        ComplaintBookEntry avisadaAyer = hoja("RC-10", "Ana Ruiz", 28);
        avisadaAyer.setDeadlineReminderAt(LocalDateTime.now().minusDays(1));
        Escenario reciente = escenario(List.of(avisadaAyer), true);
        reciente.scheduler().avisarPlazosPorVencer();
        verify(reciente.correo(), never()).enviar(anyString(), anyString(), anyString());

        ComplaintBookEntry avisadaHaceTiempo = hoja("RC-11", "Ana Ruiz", 28);
        avisadaHaceTiempo.setDeadlineReminderAt(LocalDateTime.now().minusDays(8));
        Escenario antigua = escenario(List.of(avisadaHaceTiempo), true);
        antigua.scheduler().avisarPlazosPorVencer();
        verify(antigua.correo()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void sinCorreoConfiguradoNiSiquieraConsultaLasHojas() {
        CorreoService correo = mock(CorreoService.class);
        when(correo.configurado()).thenReturn(false);
        ComplaintBookEntryRepository reclamos = mock(ComplaintBookEntryRepository.class);
        CompanySettingsRepository empresas = mock(CompanySettingsRepository.class);

        new ReclamoPlazoScheduler(empresas, reclamos, correo, mock(PlatformTransactionManager.class))
                .avisarPlazosPorVencer();

        verifyNoInteractions(reclamos, empresas);
    }

    /** Empresa con una hoja pendiente registrada hace {@code diasDesdeRegistro} días. */
    private ComplaintBookEntry hoja(String numero, String consumidor, int diasDesdeRegistro) {
        ComplaintBookEntry entry = new ComplaintBookEntry();
        entry.setEntryNumber(numero);
        entry.setConsumerName(consumidor);
        entry.setStatus(ComplaintStatus.PENDIENTE);
        entry.setCreatedAt(LocalDateTime.now().minusDays(diasDesdeRegistro));
        return entry;
    }

    /** Scheduler montado sobre una sola empresa con las hojas dadas. */
    private record Escenario(ReclamoPlazoScheduler scheduler, CorreoService correo) {
    }

    private Escenario escenario(List<ComplaintBookEntry> hojas, boolean correoSale) {
        CompanySettings empresa = new CompanySettings();
        empresa.setId(1L);
        empresa.setName("Comercial Qynex S.A.C.");
        empresa.setEmail("contacto@qynex.test");

        CompanySettingsRepository empresas = mock(CompanySettingsRepository.class);
        when(empresas.findAll()).thenReturn(List.of(empresa));

        ComplaintBookEntryRepository reclamos = mock(ComplaintBookEntryRepository.class);
        when(reclamos.findAllByStatusOrderByCreatedAtAsc(ComplaintStatus.PENDIENTE)).thenReturn(hojas);

        CorreoService correo = mock(CorreoService.class);
        when(correo.configurado()).thenReturn(true);
        when(correo.enviar(anyString(), anyString(), anyString())).thenReturn(correoSale);

        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        return new Escenario(new ReclamoPlazoScheduler(empresas, reclamos, correo, tx), correo);
    }
}
