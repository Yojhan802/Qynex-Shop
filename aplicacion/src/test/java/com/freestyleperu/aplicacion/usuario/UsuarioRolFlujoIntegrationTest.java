package com.freestyleperu.aplicacion.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.usuario.domain.Permiso;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.dto.request.ActualizarRolRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.ActualizarUsuarioRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.AsignarPermisosRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.CambiarEstadoUsuarioRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.CrearRolRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.CrearUsuarioRequest;
import com.freestyleperu.aplicacion.usuario.dto.response.PasswordTemporalResponse;
import com.freestyleperu.aplicacion.usuario.dto.response.RolResponse;
import com.freestyleperu.aplicacion.usuario.dto.response.UsuarioResponse;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.PermisoRepository;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import com.freestyleperu.aplicacion.usuario.service.RolService;
import com.freestyleperu.aplicacion.usuario.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioRolFlujoIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private RolService rolService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PermisoRepository permisoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void gestionaCicloDeVidaDeUsuariosYPermisosDeRolesConSusReglasDeNegocio() {
        Permiso permiso = permisoRepository.save(Permiso.builder()
                .code("TEST_PERMISO_USUARIO").module("TEST").description("Permiso de prueba").build());

        RolResponse rol = rolService.crear(new CrearRolRequest("VENDEDOR_TEST", "Vendedor de prueba", null));

        // No se puede repetir el código de un rol.
        assertThatThrownBy(() -> rolService.crear(new CrearRolRequest("VENDEDOR_TEST", "Otro nombre", null)))
                .isInstanceOf(RecursoDuplicadoException.class);

        UsuarioResponse usuario = usuarioService.crear(new CrearUsuarioRequest(
                "usuario.flujo.test", "usuario.flujo@test.com", "ClaveInicial123", "Usuario Flujo Test",
                null, null, List.of(rol.id())));
        assertThat(usuario.status().name()).isEqualTo("ACTIVE");
        assertThat(usuario.mustChangePassword()).isTrue();
        assertThat(usuario.roles()).extracting("id").containsExactly(rol.id());

        // No se puede repetir el username.
        assertThatThrownBy(() -> usuarioService.crear(new CrearUsuarioRequest(
                "usuario.flujo.test", "otro@test.com", "ClaveInicial123", "Otro Usuario", null, null, List.of(rol.id()))))
                .isInstanceOf(RecursoDuplicadoException.class);

        // No se puede asignar un rol inexistente.
        assertThatThrownBy(() -> usuarioService.crear(new CrearUsuarioRequest(
                "usuario.rol.invalido", null, "ClaveInicial123", "Usuario Rol Invalido", null, null, List.of(999999L))))
                .isInstanceOf(RecursoNoEncontradoException.class);

        // Actualizar datos y roles.
        UsuarioResponse actualizado = usuarioService.actualizar(usuario.id(), new ActualizarUsuarioRequest(
                "nuevo.correo@test.com", "Usuario Flujo Actualizado", null, "999888777", List.of(rol.id())));
        assertThat(actualizado.fullName()).isEqualTo("Usuario Flujo Actualizado");
        assertThat(actualizado.email()).isEqualTo("nuevo.correo@test.com");

        // Bloquear usuario limpia intentos fallidos y bloqueo temporal (RN de usuarios).
        UsuarioResponse bloqueado = usuarioService.cambiarEstado(usuario.id(), new CambiarEstadoUsuarioRequest(UsuarioEstado.BLOCKED));
        assertThat(bloqueado.status()).isEqualTo(UsuarioEstado.BLOCKED);
        assertThat(usuarioRepository.findById(usuario.id()).orElseThrow().getFailedAttempts()).isZero();

        UsuarioResponse reactivado = usuarioService.cambiarEstado(usuario.id(), new CambiarEstadoUsuarioRequest(UsuarioEstado.ACTIVE));
        assertThat(reactivado.status()).isEqualTo(UsuarioEstado.ACTIVE);

        // Resetear contraseña genera una temporal que sí valida contra el hash guardado.
        PasswordTemporalResponse passwordTemporal = usuarioService.resetPassword(usuario.id());
        assertThat(passwordTemporal.temporaryPassword()).hasSize(12);
        String hashGuardado = usuarioRepository.findById(usuario.id()).orElseThrow().getPasswordHash();
        assertThat(passwordEncoder.matches(passwordTemporal.temporaryPassword(), hashGuardado)).isTrue();
        assertThat(usuarioRepository.findById(usuario.id()).orElseThrow().isMustChangePassword()).isTrue();

        // Asignar permisos a un rol normal funciona.
        RolResponse rolConPermiso = rolService.actualizarPermisos(rol.id(), new AsignarPermisosRequest(List.of(permiso.getId())));
        assertThat(rolConPermiso.permisos()).extracting("id").containsExactly(permiso.getId());

        // Editar nombre/descripción de un rol normal funciona.
        RolResponse rolRenombrado = rolService.actualizar(rol.id(), new ActualizarRolRequest("Vendedor Renombrado", "Nueva descripción"));
        assertThat(rolRenombrado.name()).isEqualTo("Vendedor Renombrado");

        // El rol de sistema ADMINISTRADOR no puede perder sus permisos.
        Rol administrador = new Rol();
        administrador.setCode("ADMINISTRADOR");
        administrador.setName("Administrador");
        administrador.setSystem(true);
        administrador.setPermisos(new java.util.HashSet<>(List.of(permiso)));
        rolRepository.save(administrador);

        assertThatThrownBy(() -> rolService.actualizarPermisos(administrador.getId(), new AsignarPermisosRequest(List.of())))
                .isInstanceOf(OperacionNoPermitidaException.class);

        assertThat(rolService.listar()).extracting("code").contains("VENDEDOR_TEST", "ADMINISTRADOR");
        assertThat(usuarioService.listar(null, null, PageRequest.of(0, 10)).getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
