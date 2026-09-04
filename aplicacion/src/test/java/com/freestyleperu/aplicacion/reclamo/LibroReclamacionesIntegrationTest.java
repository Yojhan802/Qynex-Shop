package com.freestyleperu.aplicacion.reclamo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintBookEntry;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintType;
import com.freestyleperu.aplicacion.reclamo.dto.request.CreateComplaintRequest;
import com.freestyleperu.aplicacion.reclamo.dto.request.RespondComplaintRequest;
import com.freestyleperu.aplicacion.reclamo.dto.response.ComplaintReceiptResponse;
import com.freestyleperu.aplicacion.reclamo.dto.response.ComplaintResponse;
import com.freestyleperu.aplicacion.reclamo.dto.response.PublicComplaintResponse;
import com.freestyleperu.aplicacion.reclamo.service.ComplaintBookService;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cubre el Libro de Reclamaciones como canal de cumplimiento (D.S. 011-2011-PCM):
 * la constancia que se entrega al consumidor, los datos del proveedor que la hoja
 * debe llevar y el hecho de que consultar por número no exponga datos personales.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LibroReclamacionesIntegrationTest {

    @Autowired private ComplaintBookService complaintBookService;
    @Autowired private CompanySettingsRepository companySettingsRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private Validator validator;
    @Autowired private com.freestyleperu.aplicacion.shared.security.JwtService jwtService;
    @Autowired private com.freestyleperu.aplicacion.reclamo.repository.ComplaintBookEntryRepository complaintBookEntryRepository;
    @PersistenceContext private EntityManager entityManager;

    /**
     * El servicio resuelve la empresa por {@code TenantContext.getOrDefault()} = 1. La fila
     * se inserta con id autogenerado, y en H2 el contador avanza aunque la prueba anterior
     * haya hecho rollback, así que se fija el id a 1 para que la clase corra aislada.
     */
    private void aseguraEmpresaIdentificada() {
        CompanySettings settings = companySettingsRepository.findById(1L).orElse(null);
        if (settings == null) {
            settings = new CompanySettings();
            settings.setSlug("default");
            settings.setPlan(Plan.ECOMMERCE);
            settings.setSubscriptionStatus(SubscriptionStatus.ACTIVA);
            settings.setCurrencyCode("PEN");
            settings.setCurrencySymbol("S/");
            settings.setIgvRate(new BigDecimal("0.18"));
            settings.setShippingFlatRate(new BigDecimal("15.00"));
            settings.setReservationDepositAmount(new BigDecimal("20.00"));
            settings.setReservationExpirationDays(3);
            settings.setName("Comercial Qynex S.A.C.");
            settings.setUpdatedAt(LocalDateTime.now());
            Long generatedId = companySettingsRepository.saveAndFlush(settings).getId();
            if (!Long.valueOf(1L).equals(generatedId)) {
                entityManager.createNativeQuery("UPDATE company_settings SET id = 1 WHERE id = :id")
                        .setParameter("id", generatedId).executeUpdate();
                entityManager.clear();
            }
            settings = companySettingsRepository.findById(1L).orElseThrow();
        }
        settings.setName("Comercial Qynex S.A.C.");
        settings.setRuc("20512345678");
        settings.setAddress("Av. Grau 123, Huacho");
        settings.setUpdatedAt(LocalDateTime.now());
        companySettingsRepository.saveAndFlush(settings);
    }

    private CreateComplaintRequest hojaValida(ComplaintType tipo) {
        return new CreateComplaintRequest(tipo, "María Gutiérrez", "45678912", "maria@ejemplo.com",
                "987654321", "Jr. Lima 456, Dpto 302, Huacho", "PED-00000027", "Polo piqué talla M",
                new BigDecimal("89.90"), "El producto llegó con una costura abierta en el hombro.",
                "Solicito el cambio por otra unidad en buen estado.");
    }

    @Test
    void laConstanciaCopiaLoDeclaradoYSellaLosDatosDelProveedor() {
        aseguraEmpresaIdentificada();

        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));

        // El Art. 5 exige entregar copia de la hoja: la constancia devuelve lo declarado, no solo el código.
        assertThat(constancia.entryNumber()).startsWith("RC-");
        assertThat(constancia.type()).isEqualTo(ComplaintType.RECLAMO);
        assertThat(constancia.status()).isEqualTo(ComplaintStatus.PENDIENTE);
        assertThat(constancia.consumerName()).isEqualTo("María Gutiérrez");
        assertThat(constancia.consumerAddress()).isEqualTo("Jr. Lima 456, Dpto 302, Huacho");
        assertThat(constancia.detail()).contains("costura abierta");
        assertThat(constancia.consumerRequest()).contains("cambio por otra unidad");
        assertThat(constancia.amount()).isEqualByComparingTo("89.90");
        assertThat(constancia.orderNumber()).isEqualTo("PED-00000027");

        // La hoja identifica al proveedor con los datos de la empresa, no con lo que escriba el consumidor.
        assertThat(constancia.providerName()).isEqualTo("Comercial Qynex S.A.C.");
        assertThat(constancia.providerRuc()).isEqualTo("20512345678");
        assertThat(constancia.providerAddress()).isEqualTo("Av. Grau 123, Huacho");

        // El plazo máximo de respuesta es de 30 días calendario desde el registro.
        assertThat(constancia.responseDueDate()).isEqualTo(constancia.createdAt().toLocalDate().plusDays(30));
    }

    /**
     * El perfil de test no configura SMTP, que es justo el escenario a proteger: si el envío
     * de la constancia pudiera tumbar el registro, un SMTP caído dejaría al consumidor sin
     * reclamo Y sin constancia. La hoja se registra igual y solo queda sin sellar el envío.
     */
    @Test
    void sinSmtpConfiguradoLaHojaSeRegistraIgualYNoSeMarcaComoEnviada() {
        aseguraEmpresaIdentificada();

        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));

        assertThat(constancia.entryNumber()).startsWith("RC-");
        ComplaintBookEntry guardada = complaintBookEntryRepository
                .findByEntryNumber(constancia.entryNumber()).orElseThrow();
        assertThat(guardada.getReceiptEmailedAt()).isNull();
        assertThat(guardada.getResponseEmailedAt()).isNull();
    }

    /**
     * La constancia en PDF es la entrega efectiva mientras no haya SMTP: si el consumidor
     * no puede descargarla, no se lleva ninguna copia de lo que declaró.
     */
    @Test
    void laConstanciaSeDescargaEnPdfConElTokenQueDevuelveElRegistro() {
        aseguraEmpresaIdentificada();

        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));

        assertThat(constancia.receiptToken()).isNotBlank();
        Long hojaId = jwtService.parseComplaintReceiptToken(constancia.receiptToken(), 1L);
        assertThat(hojaId).isNotNull();

        byte[] pdf = complaintBookService.constanciaPdfDe(hojaId);
        // Encabezado de un PDF real, no un archivo vacío ni un error serializado.
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(800);
        assertThat(complaintBookService.nombrePdfDe(hojaId)).isEqualTo("constancia-" + constancia.entryNumber() + ".pdf");
    }

    /**
     * El token de la constancia lleva subject = id de la hoja. Si el filtro de sesión lo
     * aceptara, autenticaría como el usuario que tuviera ese id: hay que rechazarlo.
     */
    @Test
    void elTokenDeLaConstanciaNoSirveComoTokenDeSesionNiEnOtraTienda() {
        aseguraEmpresaIdentificada();
        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));

        assertThat(jwtService.parse(constancia.receiptToken())).isNull();
        // Y no cruza de tienda, aunque la firma sea válida.
        assertThat(jwtService.parseComplaintReceiptToken(constancia.receiptToken(), 999L)).isNull();
        assertThat(jwtService.parseComplaintReceiptToken("no-es-un-token", 1L)).isNull();
    }

    /**
     * El panel de la empresa tiene que ver el plazo legal, no solo el consumidor en su
     * constancia: los 30 días del D.S. 011-2011-PCM corren desde el registro y pasarlos es
     * sancionable. Se envía calculado desde el backend para que no haya dos cuentas
     * distintas del mismo plazo.
     */
    @Test
    void elListadoDeLaEmpresaTraeLaFechaLimiteDeRespuesta() {
        aseguraEmpresaIdentificada();
        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));

        ComplaintResponse hoja = complaintBookService.getByNumber(constancia.entryNumber());

        assertThat(hoja.responseDueDate()).isEqualTo(hoja.createdAt().toLocalDate().plusDays(30));
        // La constancia del consumidor y la vista de la empresa dicen lo mismo.
        assertThat(hoja.responseDueDate()).isEqualTo(constancia.responseDueDate());
    }

    @Test
    void consultarPorNumeroNoExponeDatosPersonalesDeOtroConsumidor() {
        aseguraEmpresaIdentificada();
        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.QUEJA));

        PublicComplaintResponse publica = complaintBookService.getPublicByNumber(constancia.entryNumber());

        // Los correlativos son predecibles, así que la consulta pública solo confirma estado.
        assertThat(publica.entryNumber()).isEqualTo(constancia.entryNumber());
        assertThat(publica.status()).isEqualTo(ComplaintStatus.PENDIENTE);
        assertThat(publica.providerName()).isEqualTo("Comercial Qynex S.A.C.");
        assertThat(publica.toString())
                .doesNotContain("maria@ejemplo.com")
                .doesNotContain("Jr. Lima 456")
                .doesNotContain("45678912");
    }

    @Test
    void elDomicilioDelConsumidorEsObligatorioPorElAnexoII() {
        CreateComplaintRequest sinDomicilio = new CreateComplaintRequest(ComplaintType.RECLAMO, "María Gutiérrez",
                "45678912", "maria@ejemplo.com", "987654321", "   ", "PED-00000027", "Polo piqué talla M",
                new BigDecimal("89.90"), "El producto llegó dañado.", "Solicito el cambio.");

        assertThat(validator.validate(sinDomicilio))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("consumerAddress"));
    }

    @Test
    void responderCierraLaHojaYUnaHojaCerradaYaNoAdmiteOtraRespuesta() {
        aseguraEmpresaIdentificada();
        Usuario staff = new Usuario();
        staff.setUsername("staff.reclamos");
        staff.setPasswordHash("hash");
        staff.setFullName("Staff Reclamos");
        staff.setStatus(UsuarioEstado.ACTIVE);
        Long staffId = usuarioRepository.save(staff).getId();

        ComplaintReceiptResponse constancia = complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));
        ComplaintResponse hoja = complaintBookService.getByNumber(constancia.entryNumber());

        ComplaintResponse respondida = complaintBookService.respond(hoja.id(),
                new RespondComplaintRequest("Se coordinó el cambio de la prenda.", true), staffId);

        assertThat(respondida.status()).isEqualTo(ComplaintStatus.CERRADO);
        assertThat(respondida.response()).contains("cambio de la prenda");
        assertThat(respondida.respondedAt()).isNotNull();
        // El domicilio viaja en la vista interna para que el staff pueda responder por escrito.
        assertThat(respondida.consumerAddress()).isEqualTo("Jr. Lima 456, Dpto 302, Huacho");

        assertThatThrownBy(() -> complaintBookService.respond(hoja.id(),
                new RespondComplaintRequest("Otra respuesta", false), staffId))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("ya está cerrada");
    }

    @Test
    void unaHojaInexistenteNoSeInventa() {
        assertThatThrownBy(() -> complaintBookService.getPublicByNumber("RC-99999999"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void elListadoInternoDevuelveLasHojasRegistradas() {
        aseguraEmpresaIdentificada();
        complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.RECLAMO));
        complaintBookService.createAndIssueReceipt(hojaValida(ComplaintType.QUEJA));

        assertThat(complaintBookService.list(PageRequest.of(0, 10)).content())
                .hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(hoja -> assertThat(hoja.providerRuc()).isEqualTo("20512345678"));
    }
}
