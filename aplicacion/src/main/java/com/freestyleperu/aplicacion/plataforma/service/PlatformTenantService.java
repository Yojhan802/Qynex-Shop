package com.freestyleperu.aplicacion.plataforma.service;

import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import com.freestyleperu.aplicacion.plataforma.dto.request.ActualizarTenantRequest;
import com.freestyleperu.aplicacion.plataforma.dto.request.CrearTenantRequest;
import com.freestyleperu.aplicacion.plataforma.dto.response.CrearTenantResponse;
import com.freestyleperu.aplicacion.plataforma.dto.response.TenantResponse;
import com.freestyleperu.aplicacion.plataforma.repository.PlatformTenantRepository;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PlatformTenantService {

    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final PlatformTenantRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PlatformTenantService(PlatformTenantRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<TenantResponse> listar(String search, SubscriptionStatus status) {
        return repository.findAll(search, status);
    }

    @Transactional
    public CrearTenantResponse crear(CrearTenantRequest request, Long actorId) {
        String slug = request.slug().trim().toLowerCase();
        String ownerUsername = request.ownerUsername().trim();
        if (repository.existsBySlug(slug)) {
            throw new RecursoDuplicadoException("Ya existe una empresa con el subdominio " + slug);
        }

        String temporaryPassword = generarPasswordTemporal();
        LocalDateTime now = LocalDateTime.now();
        try {
            Long tenantId = repository.insertTenant(request.name().trim(), slug, blankToNull(request.ruc()),
                    blankToNull(request.address()), blankToNull(request.phone()), blankToNull(request.email()),
                    request.businessVertical(), request.plan(), request.nextPaymentDue(), actorId, now);
            repository.seedTenant(tenantId, ownerUsername, blankToNull(request.ownerEmail()),
                    request.ownerFullName().trim(), passwordEncoder.encode(temporaryPassword), now);
            TenantResponse tenant = repository.findById(tenantId);
            repository.insertAudit(tenantId, actorId, "TENANT_CREADO", tenantId, now);
            return new CrearTenantResponse(tenant, ownerUsername, temporaryPassword);
        } catch (DataIntegrityViolationException ex) {
            throw new RecursoDuplicadoException("La empresa o el usuario administrador ya existe");
        }
    }

    @Transactional
    public TenantResponse actualizar(Long tenantId, ActualizarTenantRequest request, Long actorId) {
        if (!repository.existsTenant(tenantId)) {
            throw RecursoNoEncontradoException.de("Empresa", tenantId);
        }
        repository.updateTenant(tenantId, request.name().trim(), blankToNull(request.ruc()), blankToNull(request.address()),
                blankToNull(request.phone()), blankToNull(request.email()), request.businessVertical(), request.plan(), request.subscriptionStatus(),
                request.nextPaymentDue(), actorId, LocalDateTime.now());
        TenantResponse tenant = repository.findById(tenantId);
        repository.insertAudit(tenantId, actorId, "TENANT_ACTUALIZADO", tenantId, LocalDateTime.now());
        return tenant;
    }

    private String generarPasswordTemporal() {
        StringBuilder password = new StringBuilder(14);
        password.append((char) ('A' + random.nextInt(26)));
        password.append((char) ('a' + random.nextInt(26)));
        password.append(2 + random.nextInt(8));
        while (password.length() < 14) {
            password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
