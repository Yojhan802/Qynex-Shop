package com.freestyleperu.aplicacion.cliente;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.cliente.domain.Customer;
import com.freestyleperu.aplicacion.cliente.dto.response.ClienteAuthResponse;
import com.freestyleperu.aplicacion.cliente.repository.CustomerRepository;
import com.freestyleperu.aplicacion.cliente.service.ClienteAuthService;
import com.freestyleperu.aplicacion.cliente.dto.request.RegistroClienteRequest;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.AutenticacionException;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.JwtService;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ClienteAuthIntegrationTest {

    @Autowired private ClienteAuthService clienteAuthService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private JwtService jwtService;

    @Test
    void registraLoguayRefrescaConTokenQueSoloLlevaRoleCustomer() {
        ClienteAuthResponse registro = clienteAuthService.registrar(
                new RegistroClienteRequest("nuevo.cliente@test.com", "clave1234", "Nuevo Cliente", "999111222"));

        assertThat(registro.customer().email()).isEqualTo("nuevo.cliente@test.com");
        AuthenticatedUser principal = jwtService.parse(registro.accessToken());
        assertThat(principal).isNotNull();
        assertThat(principal.authorities()).containsExactly(Permisos.ROLE_CUSTOMER);

        ClienteAuthResponse login = clienteAuthService.login("nuevo.cliente@test.com", "clave1234");
        assertThat(login.customer().id()).isEqualTo(registro.customer().id());

        assertThatThrownBy(() -> clienteAuthService.login("nuevo.cliente@test.com", "clave-incorrecta"))
                .isInstanceOf(AutenticacionException.class);

        ClienteAuthResponse refrescado = clienteAuthService.refresh(login.refreshToken());
        assertThat(refrescado.customer().id()).isEqualTo(registro.customer().id());

        // El refresh token usado queda revocado — no se puede reutilizar.
        assertThatThrownBy(() -> clienteAuthService.refresh(login.refreshToken()))
                .isInstanceOf(AutenticacionException.class);
    }

    @Test
    void registrarReclamaUnClienteExistenteSinContraseñaPorCorreo() {
        Customer clienteDeTiendaFisica = new Customer();
        clienteDeTiendaFisica.setFullName("Cliente Tienda Física");
        clienteDeTiendaFisica.setEmail("compro.antes@test.com");
        clienteDeTiendaFisica.setStatus(EstadoGeneral.ACTIVE);
        Long idExistente = customerRepository.save(clienteDeTiendaFisica).getId();

        ClienteAuthResponse registro = clienteAuthService.registrar(
                new RegistroClienteRequest("compro.antes@test.com", "clave1234", "Cliente Tienda Física", "999333444"));

        // Se reclamó el mismo registro — no se creó un cliente duplicado.
        assertThat(registro.customer().id()).isEqualTo(idExistente);
        assertThat(customerRepository.findById(idExistente).orElseThrow().getPasswordHash()).isNotNull();

        assertThatThrownBy(() -> clienteAuthService.registrar(
                new RegistroClienteRequest("compro.antes@test.com", "otraClave123", "Otro Nombre", null)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void loginRechazaCuentaInactiva() {
        clienteAuthService.registrar(
                new RegistroClienteRequest("inactivo@test.com", "clave1234", "Cliente Inactivo", null));
        Customer customer = customerRepository.findByEmailIgnoreCase("inactivo@test.com").orElseThrow();
        customer.setStatus(EstadoGeneral.INACTIVE);
        customerRepository.save(customer);

        assertThatThrownBy(() -> clienteAuthService.login("inactivo@test.com", "clave1234"))
                .isInstanceOf(AutenticacionException.class);
    }
}
