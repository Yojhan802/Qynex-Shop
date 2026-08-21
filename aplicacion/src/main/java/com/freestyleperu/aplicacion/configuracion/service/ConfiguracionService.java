package com.freestyleperu.aplicacion.configuracion.service;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.dto.request.ActualizarCompanySettingsRequest;
import com.freestyleperu.aplicacion.configuracion.dto.response.CompanySettingsResponse;
import com.freestyleperu.aplicacion.configuracion.dto.response.SystemInfoResponse;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.util.ImageUploadService;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ConfiguracionService {

    private static final long SETTINGS_ID = 1L;

    private final CompanySettingsRepository companySettingsRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final ImageUploadService imageUploadService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;

    public ConfiguracionService(
            CompanySettingsRepository companySettingsRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService,
            ImageUploadService imageUploadService,
            ObjectProvider<BuildProperties> buildPropertiesProvider) {
        this.companySettingsRepository = companySettingsRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.imageUploadService = imageUploadService;
        this.buildPropertiesProvider = buildPropertiesProvider;
    }

    public CompanySettingsResponse obtener() {
        return toResponse(buscarOFallar());
    }

    @Transactional
    public CompanySettingsResponse actualizar(ActualizarCompanySettingsRequest request, Long userId) {
        CompanySettings settings = buscarOFallar();
        settings.setName(request.name());
        settings.setRuc(request.ruc());
        settings.setAddress(request.address());
        settings.setPhone(request.phone());
        settings.setEmail(request.email());
        settings.setCurrencyCode(request.currencyCode());
        settings.setCurrencySymbol(request.currencySymbol());
        settings.setIgvRate(request.igvRate());
        settings.setTicketFooter(request.ticketFooter());
        settings.setShippingFlatRate(request.shippingFlatRate());
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(userId);
        auditService.log("CONFIGURACION_ACTUALIZADA", "COMPANY_SETTINGS", SETTINGS_ID, null, request, AuditResult.SUCCESS);
        return toResponse(settings);
    }

    @Transactional
    public CompanySettingsResponse actualizarLogo(MultipartFile file, Long userId) {
        CompanySettings settings = buscarOFallar();
        settings.setLogoUrl(imageUploadService.guardar(file, "logo"));
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(userId);
        auditService.log("CONFIGURACION_LOGO_ACTUALIZADO", "COMPANY_SETTINGS", SETTINGS_ID, null, settings.getLogoUrl(), AuditResult.SUCCESS);
        return toResponse(settings);
    }

    /** Usado internamente por el módulo de pedidos para calcular el envío. */
    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public BigDecimal obtenerTarifaEnvio() {
        return buscarOFallar().getShippingFlatRate();
    }

    /** Ficha pública mínima para un panel externo de monitoreo — ver SystemInfoResponse. */
    public SystemInfoResponse obtenerInfoSistema() {
        CompanySettings settings = buscarOFallar();
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        String version = buildProperties != null ? buildProperties.getVersion() : "dev";
        return new SystemInfoResponse(settings.getName(), settings.getPlan(), version);
    }

    private CompanySettings buscarOFallar() {
        return companySettingsRepository.findById(SETTINGS_ID)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Configuración de empresa", SETTINGS_ID));
    }

    private CompanySettingsResponse toResponse(CompanySettings settings) {
        String updatedByUsername = settings.getUpdatedBy() == null
                ? null
                : usuarioRepository.findById(settings.getUpdatedBy()).map(u -> u.getUsername()).orElse(null);
        return new CompanySettingsResponse(
                settings.getName(), settings.getRuc(), settings.getAddress(), settings.getPhone(), settings.getEmail(),
                settings.getLogoUrl(), settings.getCurrencyCode(), settings.getCurrencySymbol(), settings.getIgvRate(),
                settings.getTicketFooter(), settings.getShippingFlatRate(), settings.getPlan(), settings.getUpdatedAt(), updatedByUsername);
    }
}
