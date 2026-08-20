package com.freestyleperu.aplicacion.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.freestyleperu.aplicacion.auth.dto.LoginRequest;
import com.freestyleperu.aplicacion.auth.dto.LoginResponse;
import com.freestyleperu.aplicacion.shared.exception.ApiError;
import com.freestyleperu.aplicacion.usuario.domain.Permiso;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.PermisoRepository;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica autenticación y autorización de punta a punta, contra los filtros
 * de seguridad reales (JWT + @PreAuthorize), no contra los servicios directamente
 * (docs §70 "Fase 4 — Testing / Seguridad").
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
class SeguridadIntegrationTest {

    private static final String ENDPOINT_PROTEGIDO = "/api/users";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PermisoRepository permisoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void rechazaAccesoSinTokenConCuerpoDeErrorEstandar() {
        ResponseEntity<ApiError> respuesta = restTemplate.getForEntity(ENDPOINT_PROTEGIDO, ApiError.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().status()).isEqualTo(401);
        assertThat(respuesta.getBody().error()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void rechazaTokenMalformadoOInvalido() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer esto-no-es-un-jwt-valido");
        ResponseEntity<ApiError> respuesta = restTemplate.exchange(
                ENDPOINT_PROTEGIDO, HttpMethod.GET, new HttpEntity<>(headers), ApiError.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rechazaLoginConCredencialesIncorrectas() {
        crearUsuario("seguridad.credenciales", "ClaveCorrecta123", Set.of());

        ResponseEntity<ApiError> respuesta = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("seguridad.credenciales", "ClaveIncorrecta"), ApiError.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void usuarioSinElPermisoRequeridoRecibeAccesoDenegado() {
        crearUsuario("seguridad.sinpermiso", "ClaveValida123", Set.of());
        String token = obtenerAccessToken("seguridad.sinpermiso", "ClaveValida123");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<ApiError> respuesta = restTemplate.exchange(
                ENDPOINT_PROTEGIDO, HttpMethod.GET, new HttpEntity<>(headers), ApiError.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().status()).isEqualTo(403);
    }

    @Test
    void usuarioConElPermisoRequeridoAccedeCorrectamente() {
        crearUsuario("seguridad.conpermiso", "ClaveValida123", Set.of(Permisos.USUARIOS_CONSULTAR));
        String token = obtenerAccessToken("seguridad.conpermiso", "ClaveValida123");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<String> respuesta = restTemplate.exchange(
                ENDPOINT_PROTEGIDO, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void loginYRefreshNoRequierenAutenticacionPrevia() {
        crearUsuario("seguridad.publico", "ClaveValida123", Set.of());

        ResponseEntity<LoginResponse> respuesta = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("seguridad.publico", "ClaveValida123"), LoginResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().accessToken()).isNotBlank();
        assertThat(respuesta.getBody().refreshToken()).isNotBlank();
    }

    private String obtenerAccessToken(String username, String password) {
        ResponseEntity<LoginResponse> respuesta = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest(username, password), LoginResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody().accessToken();
    }

    private Usuario crearUsuario(String username, String password, Set<String> codigosPermiso) {
        Set<Permiso> permisos = new HashSet<>();
        for (String codigo : codigosPermiso) {
            Permiso permiso = permisoRepository.findAll().stream()
                    .filter(p -> p.getCode().equals(codigo))
                    .findFirst()
                    .orElseGet(() -> permisoRepository.save(Permiso.builder().code(codigo).module("TEST").description(codigo).build()));
            permisos.add(permiso);
        }

        Rol rol = new Rol();
        rol.setCode("TEST_ROL_SEGURIDAD_" + username.hashCode());
        rol.setName("Rol de prueba seguridad");
        rol.setSystem(false);
        rol.setPermisos(permisos);
        rolRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        usuario.setFullName("Usuario de Prueba Seguridad");
        usuario.setStatus(UsuarioEstado.ACTIVE);
        usuario.setRoles(new HashSet<>(List.of(rol)));
        return usuarioRepository.save(usuario);
    }
}
