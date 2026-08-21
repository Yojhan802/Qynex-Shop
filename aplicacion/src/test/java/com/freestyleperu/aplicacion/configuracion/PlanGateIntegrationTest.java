package com.freestyleperu.aplicacion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.auth.dto.LoginRequest;
import com.freestyleperu.aplicacion.auth.dto.LoginResponse;
import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.usuario.domain.Permiso;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.dto.request.CrearUsuarioRequest;
import com.freestyleperu.aplicacion.usuario.repository.PermisoRepository;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import com.freestyleperu.aplicacion.usuario.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que @planGate bloquee rutas por plan de suscripción a nivel HTTP
 * real (mismo enfoque que SeguridadIntegrationTest, no contra los servicios
 * directamente) — ver docs/03-modelo-datos.md §15 y RN-23.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class PlanGateIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private CompanySettingsRepository companySettingsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PermisoRepository permisoRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioService usuarioService;

    @AfterEach
    void restablecerPlan() {
        establecerPlan(Plan.ECOMMERCE);
    }

    @Test
    void elCatalogoPublicoDeLaTiendaRequierePlanEcommerce() {
        establecerPlan(Plan.STARTER);
        ResponseEntity<String> bloqueado = restTemplate.getForEntity("/api/store/catalog/categories", String.class);
        assertThat(bloqueado.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        establecerPlan(Plan.ECOMMERCE);
        ResponseEntity<String> permitido = restTemplate.getForEntity("/api/store/catalog/categories", String.class);
        assertThat(permitido.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void pedidosDeStaffRequierenPlanEcommercePorEncimaDelPermiso() {
        crearUsuario("plangate.pedidos", "ClaveValida123", Set.of(Permisos.PEDIDOS_CONSULTAR));
        String token = obtenerAccessToken("plangate.pedidos", "ClaveValida123");

        establecerPlan(Plan.PROFESIONAL);
        assertThat(get("/api/orders", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        establecerPlan(Plan.ECOMMERCE);
        assertThat(get("/api/orders", token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void promotoresYAuditoriaRequierenPlanProfesionalPorEncimaDelPermiso() {
        crearUsuario("plangate.profesional", "ClaveValida123",
                Set.of(Permisos.PROMOTORES_CONSULTAR, Permisos.AUDITORIA_CONSULTAR));
        String token = obtenerAccessToken("plangate.profesional", "ClaveValida123");

        establecerPlan(Plan.STARTER);
        assertThat(get("/api/promoters", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/api/audit", token).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        establecerPlan(Plan.PROFESIONAL);
        assertThat(get("/api/promoters", token).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(get("/api/audit", token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void elPlanStarterLimitaLosUsuariosActivosYLosDemasPlanesNo() {
        establecerPlan(Plan.STARTER);
        // Se asegura llegar exactamente al límite (3), sin importar cuántos usuarios
        // hayan quedado de otras pruebas que comparten esta misma base H2.
        Rol rolBase = rolBase();
        while (usuarioRepository.count() < 3) {
            usuarioRepository.save(usuarioDirecto("plangate.relleno." + usuarioRepository.count(), rolBase));
        }

        assertThatThrownBy(() -> usuarioService.crear(new CrearUsuarioRequest(
                "plangate.rechazado", "plangate.rechazado@test.com", "ClaveValida123", "Rechazado por límite",
                null, null, List.of(rolBase.getId()))))
                .isInstanceOf(OperacionNoPermitidaException.class);

        establecerPlan(Plan.PROFESIONAL);
        var creado = usuarioService.crear(new CrearUsuarioRequest(
                "plangate.aceptado", "plangate.aceptado@test.com", "ClaveValida123", "Aceptado sin límite",
                null, null, List.of(rolBase.getId())));
        assertThat(creado.username()).isEqualTo("plangate.aceptado");
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private void establecerPlan(Plan plan) {
        CompanySettings settings = companySettingsRepository.findById(1L).orElseGet(() -> {
            CompanySettings nuevo = new CompanySettings();
            nuevo.setId(1L);
            nuevo.setName("Freestyle Perú (semilla test)");
            nuevo.setCurrencyCode("PEN");
            nuevo.setCurrencySymbol("S/");
            nuevo.setIgvRate(new BigDecimal("0.18"));
            nuevo.setShippingFlatRate(new BigDecimal("15.00"));
            nuevo.setUpdatedAt(LocalDateTime.now());
            return nuevo;
        });
        settings.setPlan(plan);
        companySettingsRepository.save(settings);
    }

    private String obtenerAccessToken(String username, String password) {
        ResponseEntity<LoginResponse> respuesta = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(username, password), LoginResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody().accessToken();
    }

    private Rol rolBase() {
        Rol rol = new Rol();
        rol.setCode("TEST_ROL_PG_" + (System.nanoTime() % 1_000_000));
        rol.setName("Rol de prueba PlanGate");
        rol.setSystem(false);
        return rolRepository.save(rol);
    }

    private Usuario usuarioDirecto(String username, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode("ClaveValida123"));
        usuario.setFullName("Relleno PlanGate");
        usuario.setStatus(UsuarioEstado.ACTIVE);
        usuario.setRoles(new HashSet<>(List.of(rol)));
        return usuario;
    }

    private void crearUsuario(String username, String password, Set<String> codigosPermiso) {
        Set<Permiso> permisos = new HashSet<>();
        for (String codigo : codigosPermiso) {
            Permiso permiso = permisoRepository.findAll().stream()
                    .filter(p -> p.getCode().equals(codigo))
                    .findFirst()
                    .orElseGet(() -> permisoRepository.save(Permiso.builder().code(codigo).module("TEST").description(codigo).build()));
            permisos.add(permiso);
        }

        Rol rol = new Rol();
        rol.setCode("TEST_ROL_PG_" + username.hashCode());
        rol.setName("Rol de prueba PlanGate");
        rol.setSystem(false);
        rol.setPermisos(permisos);
        rolRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setFullName("Usuario de Prueba PlanGate");
        usuario.setStatus(UsuarioEstado.ACTIVE);
        usuario.setRoles(new HashSet<>(List.of(rol)));
        usuarioRepository.save(usuario);
    }
}
