package com.freestyleperu.aplicacion.facturacion.service;

import com.freestyleperu.aplicacion.facturacion.domain.BillingConfiguration;
import com.freestyleperu.aplicacion.facturacion.domain.BillingProvider;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocument;
import com.freestyleperu.aplicacion.facturacion.repository.BillingConfigurationRepository;
import com.freestyleperu.aplicacion.facturacion.repository.ElectronicDocumentRepository;
import com.freestyleperu.aplicacion.pago.exception.WebhookFirmaException;
import com.freestyleperu.aplicacion.shared.security.CredentialEncryptionService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Notificaciones de Qynex CPE sobre el resultado de un comprobante ante SUNAT.
 *
 * <p>La notificacion se trata como un <b>disparador, no como fuente de verdad</b>: aunque la
 * firma sea valida, no se escribe el estado que trae el cuerpo. Se resuelve el documento y se
 * vuelve a consultar el estado a CPE, que es exactamente el mismo camino que usa el sondeo.
 * Tres motivos:
 *
 * <ul>
 *   <li>Un reenvio de una entrega antigua no puede hacer retroceder un documento que ya
 *       avanzo, porque el estado siempre sale de una consulta hecha ahora.
 *   <li>Las transiciones de estado ocurren en un unico sitio, asi que el webhook y el sondeo
 *       no pueden divergir con el tiempo.
 *   <li>Si CPE cambia el cuerpo del evento, esto no se entera: solo usa el identificador.
 * </ul>
 *
 * <p>El webhook es el canal principal porque llega al instante; el sondeo sigue activo como
 * red de seguridad, ya que ninguna entrega esta garantizada.
 */
@Service
public class QynexCpeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(QynexCpeWebhookService.class);

    /** Igual que la ventana que recomienda CPE. Mas alla, la entrega se descarta. */
    private static final long VENTANA_SEGUNDOS = 300;

    private static final String ALGORITMO = "HmacSHA256";
    private static final String PREFIJO_FIRMA = "v1=";

    private final ElectronicDocumentRepository documentRepository;
    private final BillingConfigurationRepository billingRepository;
    private final CredentialEncryptionService encryptionService;
    private final ElectronicDocumentService documentService;
    private final ObjectMapper objectMapper;

    public QynexCpeWebhookService(ElectronicDocumentRepository documentRepository,
            BillingConfigurationRepository billingRepository,
            CredentialEncryptionService encryptionService,
            ElectronicDocumentService documentService,
            ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.billingRepository = billingRepository;
        this.encryptionService = encryptionService;
        this.documentService = documentService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void procesar(String cuerpo, String firma, String timestamp) {
        BillingConfiguration billing = billingRepository.findAll().stream()
                .filter(config -> config.getProvider() == BillingProvider.QYNEX_CPE)
                .findFirst()
                .orElseThrow(() -> new WebhookFirmaException(
                        "Esta empresa no tiene Qynex CPE configurado como proveedor"));

        verificar(cuerpo, firma, timestamp, secretoDe(billing));

        String providerDocumentId = identificadorDe(cuerpo);
        ElectronicDocument document = documentRepository.findByProviderDocumentId(providerDocumentId)
                .orElse(null);
        if (document == null) {
            // Puede ser un comprobante emitido por otro sistema conectado a la misma empresa.
            // No es un error: se ignora sin ruido y se responde 204 igualmente, para que CPE
            // no lo reintente eternamente.
            log.info("Notificacion de Qynex CPE para un comprobante que no es de esta tienda: {}",
                    providerDocumentId);
            return;
        }
        // Se consulta el estado en vez de creerse el del cuerpo. Ver el Javadoc de la clase.
        documentService.actualizarEstado(document.getId());
    }

    /**
     * Comprueba la firma antes de mirar el contenido.
     *
     * <p>La comparacion es en tiempo constante: una comparacion normal termina en el primer
     * caracter distinto, y ese tiempo filtra cuanto prefijo se acerto, que es suficiente para
     * reconstruir una firma valida a base de intentos.
     */
    private void verificar(String cuerpo, String firma, String timestamp, String secreto) {
        if (firma == null || firma.isBlank() || timestamp == null || timestamp.isBlank()) {
            throw new WebhookFirmaException("La notificacion de Qynex CPE no viene firmada");
        }
        long epoch;
        try {
            epoch = Long.parseLong(timestamp.trim());
        } catch (NumberFormatException ex) {
            throw new WebhookFirmaException("La marca de tiempo de la notificacion no es valida");
        }
        long diferencia = Math.abs(Instant.now().getEpochSecond() - epoch);
        if (diferencia > VENTANA_SEGUNDOS) {
            // Sin esto, quien capture una entrega puede reproducirla mas tarde. Se compara el
            // valor absoluto para cubrir tambien un reloj adelantado.
            throw new WebhookFirmaException("La notificacion esta fuera de la ventana de tiempo aceptada");
        }
        String esperada = firmar(secreto, epoch, cuerpo);
        byte[] a = esperada.getBytes(StandardCharsets.UTF_8);
        byte[] b = firma.trim().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(a, b)) {
            throw new WebhookFirmaException("La firma de la notificacion de Qynex CPE no coincide");
        }
    }

    /** HMAC-SHA256 sobre {@code timestamp + "." + cuerpo}, en hexadecimal y con prefijo v1=. */
    private String firmar(String secreto, long epoch, String cuerpo) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] hash = mac.doFinal((epoch + "." + cuerpo).getBytes(StandardCharsets.UTF_8));
            return PREFIJO_FIRMA + HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new WebhookFirmaException("No se pudo verificar la firma de la notificacion");
        }
    }

    @SuppressWarnings("unchecked")
    private String secretoDe(BillingConfiguration billing) {
        try {
            Map<String, String> credenciales = objectMapper.readValue(
                    encryptionService.decrypt(billing.getCredentialsEncrypted()), Map.class);
            String secreto = credenciales.get("webhookSecret");
            if (secreto == null || secreto.isBlank()) {
                throw new WebhookFirmaException(
                        "Falta webhookSecret en la configuracion de Qynex CPE de esta empresa");
            }
            return secreto;
        } catch (WebhookFirmaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebhookFirmaException("No se pudieron leer las credenciales de Qynex CPE");
        }
    }

    @SuppressWarnings("unchecked")
    private String identificadorDe(String cuerpo) {
        try {
            Map<String, Object> evento = objectMapper.readValue(cuerpo, Map.class);
            Object id = evento.get("id");
            if (id == null || String.valueOf(id).isBlank()) {
                throw new WebhookFirmaException("La notificacion no identifica el comprobante");
            }
            return String.valueOf(id);
        } catch (WebhookFirmaException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new WebhookFirmaException("La notificacion de Qynex CPE no se pudo interpretar");
        }
    }
}
