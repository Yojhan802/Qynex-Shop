package com.freestyleperu.aplicacion.configuracion;

import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consultado desde {@code @PreAuthorize("@planGate.tienePlan('ECOMMERCE')")}
 * en los módulos que no están incluidos en todos los planes — mismo patrón
 * que {@code hasAuthority('PERMISO')}, pero por plan de suscripción en vez
 * de permiso de usuario. Ver docs/03-modelo-datos.md.
 */
@Component("planGate")
@Transactional(readOnly = true)
public class PlanGate {

    private static final long SETTINGS_ID = 1L;
    private static final int SIN_LIMITE = Integer.MAX_VALUE;

    private final CompanySettingsRepository companySettingsRepository;

    public PlanGate(CompanySettingsRepository companySettingsRepository) {
        this.companySettingsRepository = companySettingsRepository;
    }

    public boolean tienePlan(String minimo) {
        return planActual().ordinal() >= Plan.valueOf(minimo).ordinal();
    }

    public Plan planActual() {
        return companySettingsRepository.findById(SETTINGS_ID).map(s -> s.getPlan()).orElse(Plan.STARTER);
    }

    /** El plan Starter limita usuarios activos; los demás planes no tienen límite. */
    public int limiteUsuarios() {
        return planActual() == Plan.STARTER ? 3 : SIN_LIMITE;
    }
}
