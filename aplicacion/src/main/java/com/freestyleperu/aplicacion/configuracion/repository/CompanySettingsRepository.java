package com.freestyleperu.aplicacion.configuracion.repository;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingsRepository extends JpaRepository<CompanySettings, Long> {
}
