package com.freestyleperu.aplicacion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Ver docs/03-modelo-datos.md §15 y RN-23 — suspensión automática por falta de pago. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SuscripcionSchedulerIntegrationTest {

    @Autowired private SuscripcionScheduler suscripcionScheduler;
    @Autowired private CompanySettingsRepository companySettingsRepository;

    @Test
    void suspendeSoloCuandoSePasaLaFechaMasElMargenDeGracia() {
        sembrar(LocalDate.now().minusDays(10));
        suscripcionScheduler.revisarVencimiento();
        assertThat(companySettingsRepository.findById(1L).orElseThrow().getSubscriptionStatus())
                .isEqualTo(SubscriptionStatus.SUSPENDIDA);
    }

    @Test
    void noSuspendeDentroDelMargenDeGracia() {
        sembrar(LocalDate.now().minusDays(2));
        suscripcionScheduler.revisarVencimiento();
        assertThat(companySettingsRepository.findById(1L).orElseThrow().getSubscriptionStatus())
                .isEqualTo(SubscriptionStatus.ACTIVA);
    }

    @Test
    void noSuspendeSiNoHayFechaDePagoConfigurada() {
        sembrar(null);
        suscripcionScheduler.revisarVencimiento();
        assertThat(companySettingsRepository.findById(1L).orElseThrow().getSubscriptionStatus())
                .isEqualTo(SubscriptionStatus.ACTIVA);
    }

    private void sembrar(LocalDate nextPaymentDue) {
        CompanySettings settings = new CompanySettings();
        settings.setId(1L);
        settings.setName("Freestyle Perú (semilla test)");
        settings.setCurrencyCode("PEN");
        settings.setCurrencySymbol("S/");
        settings.setIgvRate(new BigDecimal("0.18"));
        settings.setShippingFlatRate(new BigDecimal("15.00"));
        settings.setReservationDepositAmount(new BigDecimal("20.00"));
        settings.setReservationExpirationDays(3);
        settings.setPlan(Plan.ECOMMERCE);
        settings.setSubscriptionStatus(SubscriptionStatus.ACTIVA);
        settings.setNextPaymentDue(nextPaymentDue);
        settings.setUpdatedAt(LocalDateTime.now());
        companySettingsRepository.save(settings);
    }
}
