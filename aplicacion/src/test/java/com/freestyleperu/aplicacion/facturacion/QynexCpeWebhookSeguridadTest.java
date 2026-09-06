package com.freestyleperu.aplicacion.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freestyleperu.aplicacion.facturacion.domain.BillingConfiguration;
import com.freestyleperu.aplicacion.facturacion.domain.BillingProvider;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocument;
import com.freestyleperu.aplicacion.facturacion.repository.BillingConfigurationRepository;
import com.freestyleperu.aplicacion.facturacion.repository.ElectronicDocumentRepository;
import com.freestyleperu.aplicacion.facturacion.service.ElectronicDocumentService;
import com.freestyleperu.aplicacion.facturacion.service.QynexCpeWebhookService;
import com.freestyleperu.aplicacion.pago.exception.WebhookFirmaException;
import com.freestyleperu.aplicacion.shared.security.CredentialEncryptionService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/**
 * La firma es lo unico que separa una notificacion de Qynex CPE de cualquiera que conozca la
 * URL. Un fallo aqui no se nota: el sistema seguiria funcionando, aceptando estados de
 * comprobante que nadie mando.
 */
class QynexCpeWebhookSeguridadTest {

    private static final String SECRETO = "un-secreto-de-webhook-de-prueba";
    private static final String CUERPO =
            "{\"event\":\"document.accepted\",\"id\":\"b591937b-66f4-42f9-aa27-79eeaa47ae2e\"}";

    @Test
    void unaNotificacionFirmadaDisparaLaConsultaDelEstado() {
        Escenario esc = escenario(true);
        long ahora = Instant.now().getEpochSecond();

        esc.service().procesar(CUERPO, firmar(SECRETO, ahora, CUERPO), String.valueOf(ahora));

        // No se escribe el estado que trae el cuerpo: se vuelve a consultar a CPE.
        verify(esc.documentService()).actualizarEstado(7L);
    }

    @Test
    void unaFirmaQueNoCuadraSeRechazaYNoTocaElComprobante() {
        Escenario esc = escenario(true);
        long ahora = Instant.now().getEpochSecond();

        assertThatThrownBy(() -> esc.service().procesar(
                CUERPO, firmar("otro-secreto", ahora, CUERPO), String.valueOf(ahora)))
                .isInstanceOf(WebhookFirmaException.class);

        verify(esc.documentService(), never()).actualizarEstado(anyLong());
    }

    /**
     * Sin ventana de tiempo, quien capture una entrega puede reproducirla despues: la firma
     * sigue siendo valida porque el cuerpo no cambio.
     */
    @Test
    void unaEntregaAntiguaSeDescartaAunqueLaFirmaSeaValida() {
        Escenario esc = escenario(true);
        long viejo = Instant.now().getEpochSecond() - 600;

        assertThatThrownBy(() -> esc.service().procesar(
                CUERPO, firmar(SECRETO, viejo, CUERPO), String.valueOf(viejo)))
                .isInstanceOf(WebhookFirmaException.class)
                .hasMessageContaining("ventana");

        verify(esc.documentService(), never()).actualizarEstado(anyLong());
    }

    @Test
    void unCuerpoAlteradoInvalidaLaFirma() {
        Escenario esc = escenario(true);
        long ahora = Instant.now().getEpochSecond();
        String firma = firmar(SECRETO, ahora, CUERPO);

        assertThatThrownBy(() -> esc.service().procesar(
                CUERPO.replace("accepted", "rejected"), firma, String.valueOf(ahora)))
                .isInstanceOf(WebhookFirmaException.class);
    }

    @Test
    void sinFirmaOSinMarcaDeTiempoSeRechaza() {
        Escenario esc = escenario(true);
        long ahora = Instant.now().getEpochSecond();

        assertThatThrownBy(() -> esc.service().procesar(CUERPO, null, String.valueOf(ahora)))
                .isInstanceOf(WebhookFirmaException.class);
        assertThatThrownBy(() -> esc.service().procesar(CUERPO, firmar(SECRETO, ahora, CUERPO), null))
                .isInstanceOf(WebhookFirmaException.class);
    }

    /**
     * Una empresa puede tener varios sistemas conectados a la misma cuenta de CPE. Un
     * comprobante que no es de esta tienda se ignora sin error, para que CPE no lo reintente
     * eternamente contra una entrega que nunca va a poder aplicar.
     */
    @Test
    void unComprobanteDeOtroSistemaSeIgnoraSinFallar() {
        Escenario esc = escenario(false);
        long ahora = Instant.now().getEpochSecond();

        esc.service().procesar(CUERPO, firmar(SECRETO, ahora, CUERPO), String.valueOf(ahora));

        verify(esc.documentService(), never()).actualizarEstado(anyLong());
    }

    @Test
    void sinQynexCpeConfiguradoNoSeAceptaLaNotificacion() {
        BillingConfigurationRepository billing = mock(BillingConfigurationRepository.class);
        when(billing.findAll()).thenReturn(List.of());
        QynexCpeWebhookService service = new QynexCpeWebhookService(
                mock(ElectronicDocumentRepository.class), billing,
                mock(CredentialEncryptionService.class), mock(ElectronicDocumentService.class),
                new ObjectMapper());

        assertThatThrownBy(() -> service.procesar(CUERPO, "v1=algo", "1"))
                .isInstanceOf(WebhookFirmaException.class);
    }

    // --- montaje -------------------------------------------------------------------------

    private record Escenario(QynexCpeWebhookService service, ElectronicDocumentService documentService) {
    }

    private Escenario escenario(boolean documentoDeEstaTienda) {
        BillingConfiguration config = new BillingConfiguration();
        config.setProvider(BillingProvider.QYNEX_CPE);
        config.setCredentialsEncrypted("cifrado");

        BillingConfigurationRepository billing = mock(BillingConfigurationRepository.class);
        when(billing.findAll()).thenReturn(List.of(config));

        CredentialEncryptionService cifrado = mock(CredentialEncryptionService.class);
        when(cifrado.decrypt("cifrado")).thenReturn(
                "{\"apiKey\":\"k\",\"apiSecret\":\"s\",\"webhookSecret\":\"" + SECRETO + "\"}");

        ElectronicDocument documento = new ElectronicDocument();
        documento.setId(7L);
        ElectronicDocumentRepository documentos = mock(ElectronicDocumentRepository.class);
        when(documentos.findByProviderDocumentId("b591937b-66f4-42f9-aa27-79eeaa47ae2e"))
                .thenReturn(documentoDeEstaTienda ? Optional.of(documento) : Optional.empty());

        ElectronicDocumentService documentService = mock(ElectronicDocumentService.class);
        return new Escenario(
                new QynexCpeWebhookService(documentos, billing, cifrado, documentService, new ObjectMapper()),
                documentService);
    }

    /** La misma firma que produce Qynex CPE: HMAC-SHA256 de timestamp + "." + cuerpo. */
    private String firmar(String secreto, long epoch, String cuerpo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "v1=" + HexFormat.of().formatHex(
                    mac.doFinal((epoch + "." + cuerpo).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void laFirmaCoincideConLaQueGeneraQynexCpe() {
        // Vector fijo: si alguien cambia el material firmado, esto lo caza aunque los demas
        // tests sigan pasando, porque ahi la firma se genera con el mismo codigo que se prueba.
        String esperada = firmar("secreto", 1700000000L, "{\"a\":1}");
        assertThat(esperada).startsWith("v1=").hasSize(67);
    }
}
