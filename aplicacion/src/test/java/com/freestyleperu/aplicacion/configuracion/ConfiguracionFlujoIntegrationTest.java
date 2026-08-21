package com.freestyleperu.aplicacion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.dto.request.ActualizarCompanySettingsRequest;
import com.freestyleperu.aplicacion.configuracion.dto.response.CompanySettingsResponse;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.configuracion.service.ConfiguracionService;
import com.freestyleperu.aplicacion.shared.exception.ArchivoInvalidoException;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfiguracionFlujoIntegrationTest {

    @Autowired private ConfiguracionService configuracionService;
    @Autowired private CompanySettingsRepository companySettingsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;

    @Test
    void actualizaDatosDeEmpresaYLogoRespetandoElTipoDeArchivo() {
        sembrarFilaUnica();
        Long userId = nuevoUsuario().getId();

        CompanySettingsResponse actual = configuracionService.obtener();
        assertThat(actual.name()).isEqualTo("Freestyle Perú (semilla test)");
        assertThat(actual.updatedByUsername()).isNull();

        CompanySettingsResponse actualizado = configuracionService.actualizar(new ActualizarCompanySettingsRequest(
                "Freestyle Perú SAC", "20123456789", "Av. Test 123", "999888777", "contacto@test.com",
                "PEN", "S/", new BigDecimal("0.18"), "Gracias por su compra", new BigDecimal("18.00")), userId);

        assertThat(actualizado.name()).isEqualTo("Freestyle Perú SAC");
        assertThat(actualizado.ruc()).isEqualTo("20123456789");
        assertThat(actualizado.igvRate()).isEqualByComparingTo("0.18");
        assertThat(actualizado.shippingFlatRate()).isEqualByComparingTo("18.00");
        assertThat(actualizado.updatedByUsername()).isEqualTo("config.test");

        // El logo debe ser de un tipo de imagen soportado.
        MockMultipartFile archivoInvalido = new MockMultipartFile("file", "documento.pdf", "application/pdf", "contenido".getBytes());
        assertThatThrownBy(() -> configuracionService.actualizarLogo(archivoInvalido, userId))
                .isInstanceOf(ArchivoInvalidoException.class);

        // Un archivo vacío también se rechaza.
        MockMultipartFile archivoVacio = new MockMultipartFile("file", "vacio.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> configuracionService.actualizarLogo(archivoVacio, userId))
                .isInstanceOf(ArchivoInvalidoException.class);

        // Un PNG válido se guarda y actualiza la URL del logo.
        MockMultipartFile logoValido = new MockMultipartFile("file", "logo.png", "image/png", new byte[] { 1, 2, 3, 4 });
        CompanySettingsResponse conLogo = configuracionService.actualizarLogo(logoValido, userId);
        assertThat(conLogo.logoUrl()).startsWith("/uploads/logo/").endsWith(".png");
    }

    private void sembrarFilaUnica() {
        CompanySettings settings = new CompanySettings();
        settings.setId(1L);
        settings.setName("Freestyle Perú (semilla test)");
        settings.setCurrencyCode("PEN");
        settings.setCurrencySymbol("S/");
        settings.setIgvRate(new BigDecimal("0.18"));
        settings.setShippingFlatRate(new BigDecimal("15.00"));
        settings.setUpdatedAt(LocalDateTime.now());
        companySettingsRepository.save(settings);
    }

    private Usuario nuevoUsuario() {
        Rol rol = new Rol();
        rol.setCode("TEST_ROL_CONFIG");
        rol.setName("Rol de prueba configuración");
        rol.setSystem(false);
        rolRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setUsername("config.test");
        usuario.setPasswordHash("hash");
        usuario.setFullName("Usuario Configuración Test");
        usuario.setStatus(UsuarioEstado.ACTIVE);
        usuario.setRoles(new HashSet<>(List.of(rol)));
        return usuarioRepository.save(usuario);
    }
}
