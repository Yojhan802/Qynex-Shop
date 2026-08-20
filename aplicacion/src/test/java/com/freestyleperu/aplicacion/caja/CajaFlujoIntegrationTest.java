package com.freestyleperu.aplicacion.caja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.caja.domain.CashMovementType;
import com.freestyleperu.aplicacion.caja.domain.CashRegister;
import com.freestyleperu.aplicacion.caja.dto.request.AbrirCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.request.CerrarCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.request.MovimientoCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.response.MovimientoCajaResponse;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaDetalleResponse;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaResponse;
import com.freestyleperu.aplicacion.caja.repository.CashRegisterRepository;
import com.freestyleperu.aplicacion.caja.service.CajaService;
import com.freestyleperu.aplicacion.inventario.domain.Branch;
import com.freestyleperu.aplicacion.inventario.repository.BranchRepository;
import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CajaFlujoIntegrationTest {

    @Autowired private BranchRepository branchRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CajaService cajaService;

    @Test
    void abreRegistraMovimientosYCierraConDiferenciaCalculadaCorrectamente() {
        CashRegister caja = nuevaCaja();
        Long userId = nuevoUsuario("cajero.test").getId();

        SesionCajaResponse sesion = cajaService.abrirCaja(new AbrirCajaRequest(caja.getId(), new BigDecimal("300.00")), userId);
        assertThat(sesion.status().name()).isEqualTo("OPEN");

        // No se puede abrir dos sesiones sobre la misma caja.
        assertThatThrownBy(() -> cajaService.abrirCaja(new AbrirCajaRequest(caja.getId(), BigDecimal.TEN), userId))
                .isInstanceOf(ReglaDeNegocioException.class);

        // El endpoint manual no admite VENTA (reservado para el módulo de ventas).
        assertThatThrownBy(() -> cajaService.registrarMovimiento(
                new MovimientoCajaRequest(CashMovementType.VENTA, new BigDecimal("50.00"), "intento inválido"), userId))
                .isInstanceOf(OperacionNoPermitidaException.class);

        MovimientoCajaResponse ingreso = cajaService.registrarMovimiento(
                new MovimientoCajaRequest(CashMovementType.INGRESO, new BigDecimal("100.00"), "Vuelto adicional"), userId);
        assertThat(ingreso.amount()).isEqualByComparingTo("100.00");

        MovimientoCajaResponse retiro = cajaService.registrarMovimiento(
                new MovimientoCajaRequest(CashMovementType.RETIRO, new BigDecimal("30.00"), "Depósito al banco"), userId);
        assertThat(retiro.amount()).isEqualByComparingTo("-30.00");

        // Esperado: 300 (apertura) + 100 (ingreso) - 30 (retiro) = 370.
        var resumen = cajaService.obtenerResumenCierre(sesion.id());
        assertThat(resumen.expectedAmount()).isEqualByComparingTo("370.00");

        SesionCajaResponse cerrada = cajaService.cerrarCaja(sesion.id(),
                new CerrarCajaRequest(new BigDecimal("365.00"), "Faltante detectado en el turno"), userId);
        assertThat(cerrada.status().name()).isEqualTo("CLOSED");
        assertThat(cerrada.expectedAmount()).isEqualByComparingTo("370.00");
        assertThat(cerrada.difference()).isEqualByComparingTo("-5.00");

        // Una caja cerrada no admite más movimientos (RN-11).
        assertThatThrownBy(() -> cajaService.registrarMovimiento(
                new MovimientoCajaRequest(CashMovementType.INGRESO, BigDecimal.TEN, "no debería aplicarse"), userId))
                .isInstanceOf(ReglaDeNegocioException.class);

        SesionCajaDetalleResponse detalle = cajaService.obtenerSesion(sesion.id());
        assertThat(detalle.movements()).hasSize(2);
    }

    private CashRegister nuevaCaja() {
        Branch branch = new Branch();
        branch.setCode("SUC-CAJA-TEST");
        branch.setName("Sucursal caja test");
        branchRepository.save(branch);

        CashRegister register = new CashRegister();
        register.setBranch(branch);
        register.setCode("CAJA-TEST");
        register.setName("Caja de prueba");
        return cashRegisterRepository.save(register);
    }

    private Usuario nuevoUsuario(String username) {
        Rol rol = new Rol();
        rol.setCode("TEST_ROL_CAJA");
        rol.setName("Rol de prueba caja");
        rol.setSystem(false);
        rolRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash("hash");
        usuario.setFullName("Cajero de Prueba");
        usuario.setStatus(UsuarioEstado.ACTIVE);
        usuario.setRoles(new HashSet<>(List.of(rol)));
        return usuarioRepository.save(usuario);
    }
}
