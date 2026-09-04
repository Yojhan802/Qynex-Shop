package com.freestyleperu.aplicacion.reclamo.service;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintBookEntry;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import com.freestyleperu.aplicacion.reclamo.dto.request.CreateComplaintRequest;
import com.freestyleperu.aplicacion.reclamo.dto.request.RespondComplaintRequest;
import com.freestyleperu.aplicacion.reclamo.dto.response.ComplaintReceiptResponse;
import com.freestyleperu.aplicacion.reclamo.dto.response.ComplaintResponse;
import com.freestyleperu.aplicacion.reclamo.dto.response.PublicComplaintResponse;
import com.freestyleperu.aplicacion.reclamo.repository.ComplaintBookEntryRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.correo.CorreoService;
import com.freestyleperu.aplicacion.shared.security.JwtService;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.security.TenantContext;
import com.freestyleperu.aplicacion.shared.util.SequenceService;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ComplaintBookService {

    private final ComplaintBookEntryRepository repository;
    private final CompanySettingsRepository companySettingsRepository;
    private final UsuarioRepository usuarioRepository;
    private final SequenceService sequenceService;
    private final AuditService auditService;
    private final CorreoService correoService;
    private final ConstanciaPdfService constanciaPdfService;
    private final JwtService jwtService;

    public ComplaintBookService(ComplaintBookEntryRepository repository,
            CompanySettingsRepository companySettingsRepository, UsuarioRepository usuarioRepository,
            SequenceService sequenceService, AuditService auditService, CorreoService correoService,
            ConstanciaPdfService constanciaPdfService, JwtService jwtService) {
        this.repository = repository;
        this.companySettingsRepository = companySettingsRepository;
        this.usuarioRepository = usuarioRepository;
        this.sequenceService = sequenceService;
        this.auditService = auditService;
        this.correoService = correoService;
        this.constanciaPdfService = constanciaPdfService;
        this.jwtService = jwtService;
    }

    /**
     * Plazo máximo de respuesta al consumidor (D.S. 011-2011-PCM, Art. 5, modificado
     * por el D.S. 006-2014-PCM). Se calcula sobre días calendario.
     */
    private static final int PLAZO_RESPUESTA_DIAS = 30;

    @Transactional
    public ComplaintReceiptResponse createAndIssueReceipt(CreateComplaintRequest request) {
        ComplaintBookEntry entry = createEntry(request);
        // El envío no puede tumbar el registro: CorreoService no lanza, devuelve si salió.
        // Si no salió, la hoja queda igual de registrada y el consumidor conserva la
        // descarga en pantalla, que es la entrega efectiva mientras no haya SMTP.
        boolean enviado = correoService.enviar(entry.getConsumerEmail(), asuntoConstancia(entry),
                cuerpoConstancia(entry), nombrePdf(entry), constanciaPdf(entry));
        if (enviado) {
            entry.setReceiptEmailedAt(LocalDateTime.now());
        }
        return toReceipt(entry, jwtService.generateComplaintReceiptToken(entry.getId(), TenantContext.getOrDefault()));
    }

    private ComplaintBookEntry createEntry(CreateComplaintRequest request) {
        CompanySettings company = companySettingsRepository.findById(TenantContext.getOrDefault())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Configuración de empresa", TenantContext.getOrDefault()));
        ComplaintBookEntry entry = new ComplaintBookEntry();
        entry.setEntryNumber(sequenceService.next("RECLAMO", "RC", 8));
        entry.setType(request.type());
        entry.setStatus(ComplaintStatus.PENDIENTE);
        entry.setProviderName(company.getName());
        entry.setProviderRuc(company.getRuc());
        entry.setProviderAddress(company.getAddress());
        entry.setConsumerName(request.consumerName().trim());
        entry.setConsumerDocument(blankToNull(request.consumerDocument()));
        entry.setConsumerEmail(request.consumerEmail().trim().toLowerCase());
        entry.setConsumerPhone(blankToNull(request.consumerPhone()));
        entry.setConsumerAddress(request.consumerAddress().trim());
        entry.setOrderNumber(blankToNull(request.orderNumber()));
        entry.setProductServiceDescription(request.productServiceDescription().trim());
        entry.setAmount(request.amount());
        entry.setDetail(request.detail().trim());
        entry.setConsumerRequest(request.consumerRequest().trim());
        ComplaintBookEntry saved = repository.save(entry);
        auditService.log("RECLAMO_CREADO", "RECLAMO", saved.getId(), null,
                new Object[] { saved.getEntryNumber(), saved.getType() }, AuditResult.SUCCESS);
        return saved;
    }

    public PageResponse<ComplaintResponse> list(Pageable pageable) {
        return PageResponse.of(repository.findAllByOrderByCreatedAtDesc(pageable), this::toResponse);
    }

    public ComplaintResponse getByNumber(String entryNumber) {
        return toResponse(repository.findByEntryNumber(entryNumber.trim().toUpperCase())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Hoja de reclamación", entryNumber)));
    }

    public PublicComplaintResponse getPublicByNumber(String entryNumber) {
        ComplaintBookEntry entry = repository.findByEntryNumber(entryNumber.trim().toUpperCase())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Hoja de reclamación", entryNumber));
        return new PublicComplaintResponse(entry.getEntryNumber(), entry.getType(), entry.getStatus(),
                entry.getProviderName(), entry.getResponse(), entry.getCreatedAt(), entry.getRespondedAt());
    }

    @Transactional
    public ComplaintResponse respond(Long id, RespondComplaintRequest request, Long userId) {
        ComplaintBookEntry entry = repository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Hoja de reclamación", id));
        if (entry.getStatus() == ComplaintStatus.CERRADO) {
            throw new ReglaDeNegocioException("La hoja de reclamación ya está cerrada");
        }
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", userId));
        entry.setResponse(request.response().trim());
        entry.setRespondedAt(LocalDateTime.now());
        entry.setRespondedBy(user);
        entry.setStatus(request.close() ? ComplaintStatus.CERRADO : ComplaintStatus.RESPONDIDO);
        auditService.log("RECLAMO_RESPONDIDO", "RECLAMO", entry.getId(), null, entry.getStatus(), AuditResult.SUCCESS);
        // Responder al consumidor es la obligación; dejarla anotada en el sistema no le
        // llega a nadie. Si el correo no sale, la respuesta queda guardada igual.
        if (correoService.enviar(entry.getConsumerEmail(), asuntoRespuesta(entry), cuerpoRespuesta(entry))) {
            entry.setResponseEmailedAt(LocalDateTime.now());
        }
        return toResponse(entry);
    }

    private ComplaintResponse toResponse(ComplaintBookEntry entry) {
        return new ComplaintResponse(entry.getId(), entry.getEntryNumber(), entry.getType(), entry.getStatus(),
                entry.getProviderName(), entry.getProviderRuc(), entry.getProviderAddress(), entry.getConsumerName(),
                entry.getConsumerDocument(), entry.getConsumerEmail(), entry.getConsumerPhone(),
                entry.getConsumerAddress(), entry.getOrderNumber(),
                entry.getSaleNumber(), entry.getProductServiceDescription(), entry.getAmount(), entry.getDetail(),
                entry.getConsumerRequest(), entry.getResponse(), entry.getCreatedAt(), entry.getRespondedAt(),
                entry.getCreatedAt().toLocalDate().plusDays(PLAZO_RESPUESTA_DIAS));
    }

    /** Constancia en PDF de una hoja, para el endpoint de descarga. */
    public byte[] constanciaPdfDe(Long complaintId) {
        return constanciaPdf(repository.findById(complaintId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Hoja de reclamación", complaintId)));
    }

    public String nombrePdfDe(Long complaintId) {
        return nombrePdf(repository.findById(complaintId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Hoja de reclamación", complaintId)));
    }

    private byte[] constanciaPdf(ComplaintBookEntry entry) {
        return constanciaPdfService.generar(entry, PLAZO_RESPUESTA_DIAS);
    }

    private String nombrePdf(ComplaintBookEntry entry) {
        return "constancia-" + entry.getEntryNumber() + ".pdf";
    }

    private ComplaintReceiptResponse toReceipt(ComplaintBookEntry entry, String receiptToken) {
        return new ComplaintReceiptResponse(entry.getEntryNumber(), entry.getType(), entry.getStatus(),
                entry.getProviderName(), entry.getProviderRuc(), entry.getProviderAddress(), entry.getConsumerName(),
                entry.getConsumerDocument(), entry.getConsumerEmail(), entry.getConsumerPhone(),
                entry.getConsumerAddress(), entry.getOrderNumber(), entry.getProductServiceDescription(),
                entry.getAmount(), entry.getDetail(), entry.getConsumerRequest(), entry.getCreatedAt(),
                entry.getCreatedAt().toLocalDate().plusDays(PLAZO_RESPUESTA_DIAS), receiptToken);
    }

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String asuntoConstancia(ComplaintBookEntry entry) {
        return "Constancia de tu " + entry.getType().name().toLowerCase() + " " + entry.getEntryNumber();
    }

    private String asuntoRespuesta(ComplaintBookEntry entry) {
        return "Respuesta a tu " + entry.getType().name().toLowerCase() + " " + entry.getEntryNumber();
    }

    /**
     * Copia íntegra de lo declarado, como exige el Art. 5: el consumidor debe poder probar
     * qué presentó y cuándo, sin depender de que la página siguiera abierta.
     */
    private String cuerpoConstancia(ComplaintBookEntry entry) {
        LocalDate vence = entry.getCreatedAt().toLocalDate().plusDays(PLAZO_RESPUESTA_DIAS);
        StringBuilder texto = new StringBuilder();
        linea(texto, entry.getConsumerName() + ":");
        linea(texto, "");
        linea(texto, "Registramos tu " + entry.getType().name().toLowerCase()
                + " en nuestro Libro de Reclamaciones. Guarda este correo: es tu constancia.");
        linea(texto, "");
        linea(texto, "HOJA N° " + entry.getEntryNumber());
        linea(texto, "Fecha: " + entry.getCreatedAt().format(FECHA));
        linea(texto, "Tipo: " + entry.getType());
        linea(texto, "");
        linea(texto, "PROVEEDOR");
        linea(texto, entry.getProviderName());
        if (entry.getProviderRuc() != null) linea(texto, "RUC " + entry.getProviderRuc());
        if (entry.getProviderAddress() != null) linea(texto, entry.getProviderAddress());
        linea(texto, "");
        linea(texto, "CONSUMIDOR");
        linea(texto, entry.getConsumerName());
        if (entry.getConsumerDocument() != null) linea(texto, "Documento: " + entry.getConsumerDocument());
        linea(texto, "Domicilio: " + entry.getConsumerAddress());
        linea(texto, "Correo: " + entry.getConsumerEmail());
        if (entry.getConsumerPhone() != null) linea(texto, "Teléfono: " + entry.getConsumerPhone());
        linea(texto, "");
        linea(texto, "BIEN CONTRATADO");
        linea(texto, entry.getProductServiceDescription());
        if (entry.getOrderNumber() != null) linea(texto, "Pedido: " + entry.getOrderNumber());
        if (entry.getAmount() != null) linea(texto, "Monto: " + entry.getAmount());
        linea(texto, "");
        linea(texto, "DETALLE");
        linea(texto, entry.getDetail());
        linea(texto, "");
        linea(texto, "LO QUE PIDES");
        linea(texto, entry.getConsumerRequest());
        linea(texto, "");
        linea(texto, "---");
        linea(texto, "Te responderemos como máximo el " + vence.format(DIA)
                + " (30 días calendario, D.S. 011-2011-PCM).");
        linea(texto, "Este reclamo no constituye una denuncia ante INDECOPI, y presentarlo no impide "
                + "que acudas a las demás vías que la ley te reconoce.");
        return texto.toString();
    }

    private String cuerpoRespuesta(ComplaintBookEntry entry) {
        StringBuilder texto = new StringBuilder();
        linea(texto, entry.getConsumerName() + ":");
        linea(texto, "");
        linea(texto, "Respondemos a tu " + entry.getType().name().toLowerCase() + " "
                + entry.getEntryNumber() + ", presentada el " + entry.getCreatedAt().format(FECHA) + ".");
        linea(texto, "");
        linea(texto, "RESPUESTA DE " + entry.getProviderName());
        linea(texto, entry.getResponse());
        linea(texto, "");
        linea(texto, "---");
        linea(texto, "Si no estás conforme, puedes acudir a INDECOPI o a las demás vías que la ley "
                + "te reconoce: nuestra respuesta no agota tus derechos.");
        return texto.toString();
    }

    private void linea(StringBuilder destino, String texto) {
        destino.append(texto).append("\n");
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
