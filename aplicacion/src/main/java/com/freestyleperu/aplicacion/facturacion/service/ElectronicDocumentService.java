package com.freestyleperu.aplicacion.facturacion.service;

import com.freestyleperu.aplicacion.configuracion.domain.CompanySettings;
import com.freestyleperu.aplicacion.configuracion.repository.CompanySettingsRepository;
import com.freestyleperu.aplicacion.facturacion.domain.BillingConfiguration;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocument;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocumentStatus;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocumentType;
import com.freestyleperu.aplicacion.facturacion.dto.request.CrearElectronicDocumentRequest;
import com.freestyleperu.aplicacion.facturacion.dto.response.ElectronicDocumentResponse;
import com.freestyleperu.aplicacion.facturacion.exception.ProveedorFacturacionException;
import com.freestyleperu.aplicacion.facturacion.port.BillingConfigurationData;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingCommand;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingProvider;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingResult;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingResource;
import com.freestyleperu.aplicacion.facturacion.repository.BillingConfigurationRepository;
import com.freestyleperu.aplicacion.facturacion.repository.ElectronicDocumentRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.security.CredentialEncryptionService;
import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.security.TenantContext;
import com.freestyleperu.aplicacion.venta.domain.Sale;
import com.freestyleperu.aplicacion.venta.domain.SaleDetail;
import com.freestyleperu.aplicacion.venta.domain.SaleStatus;
import com.freestyleperu.aplicacion.venta.repository.SaleDetailRepository;
import com.freestyleperu.aplicacion.venta.repository.SaleRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class ElectronicDocumentService {

    private final ElectronicDocumentRepository documentRepository;
    private final BillingConfigurationRepository billingConfigurationRepository;
    private final CompanySettingsRepository companySettingsRepository;
    private final SaleRepository saleRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final CredentialEncryptionService encryptionService;
    private final Map<com.freestyleperu.aplicacion.facturacion.domain.BillingProvider, ElectronicInvoicingProvider> invoicingProviders;

    public ElectronicDocumentService(
            ElectronicDocumentRepository documentRepository,
            BillingConfigurationRepository billingConfigurationRepository,
            CompanySettingsRepository companySettingsRepository,
            SaleRepository saleRepository,
            SaleDetailRepository saleDetailRepository,
            AuditService auditService,
            ObjectMapper objectMapper,
            CredentialEncryptionService encryptionService,
            List<ElectronicInvoicingProvider> invoicingProviders) {
        this.documentRepository = documentRepository;
        this.billingConfigurationRepository = billingConfigurationRepository;
        this.companySettingsRepository = companySettingsRepository;
        this.saleRepository = saleRepository;
        this.saleDetailRepository = saleDetailRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
        this.invoicingProviders = invoicingProviders.stream()
                .collect(Collectors.toUnmodifiableMap(ElectronicInvoicingProvider::type, Function.identity()));
    }

    public List<ElectronicDocumentResponse> listarPorVenta(Long saleId) {
        return documentRepository.findAllBySaleIdOrderByCreatedAtDesc(saleId).stream().map(this::toResponse).toList();
    }

    /**
     * Garantiza la anulacion fiscal de una venta que ya tiene una boleta o factura
     * aceptada. La anulacion de un CPE no se hace cambiando su estado local: se
     * emite una nota de credito por "Anulacion de la operacion" (codigo 01).
     *
     * Si Verifac responde de forma asincrona, la venta permanece intacta y la
     * siguiente solicitud vuelve a consultar el estado de la nota antes de
     * permitir que VentaService revierta stock y caja.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
            noRollbackFor = ReglaDeNegocioException.class)
    public void asegurarNotaCreditoAnulacion(Long saleId, String reason, Long userId) {
        List<ElectronicDocument> documents = documentRepository.findAllBySaleIdOrderByCreatedAtDesc(saleId);
        List<ElectronicDocument> acceptedSources = documents.stream()
                .filter(document -> document.getDocumentType() == ElectronicDocumentType.BOLETA
                        || document.getDocumentType() == ElectronicDocumentType.FACTURA)
                .filter(document -> document.getStatus() == ElectronicDocumentStatus.ACCEPTED)
                .toList();

        if (acceptedSources.isEmpty()) {
            boolean inProgress = documents.stream()
                    .anyMatch(document -> (document.getDocumentType() == ElectronicDocumentType.BOLETA
                            || document.getDocumentType() == ElectronicDocumentType.FACTURA)
                            && (document.getStatus() == ElectronicDocumentStatus.DRAFT
                                    || document.getStatus() == ElectronicDocumentStatus.PENDING
                                    || document.getStatus() == ElectronicDocumentStatus.SENT));
            if (inProgress) {
                throw new ReglaDeNegocioException(
                        "No se puede anular la venta mientras su comprobante electronico esta en proceso");
            }
            return;
        }

        if (acceptedSources.size() > 1) {
            throw new ReglaDeNegocioException(
                    "La venta tiene mas de un comprobante aceptado; la anulacion fiscal requiere revision manual");
        }

        ElectronicDocument source = acceptedSources.get(0);
        ElectronicDocument cancellationNote = documents.stream()
                .filter(document -> document.getDocumentType() == ElectronicDocumentType.NOTA_CREDITO)
                .filter(document -> document.getSourceDocument() != null
                        && source.getId().equals(document.getSourceDocument().getId()))
                .filter(document -> "01".equals(reason(document, "reasonCode")))
                .findFirst()
                .orElse(null);

        ElectronicDocumentResponse result;
        if (cancellationNote == null) {
            result = crearBorrador(
                    saleId,
                    new CrearElectronicDocumentRequest(
                            ElectronicDocumentType.NOTA_CREDITO,
                            source.getId(),
                            "01",
                            reason,
                            null),
                    "sale-" + saleId + "-cancel-note-" + source.getId(),
                    userId);
            result = enviar(result.id(), userId);
        } else if (cancellationNote.getStatus() == ElectronicDocumentStatus.DRAFT) {
            result = enviar(cancellationNote.getId(), userId);
        } else if (cancellationNote.getStatus() == ElectronicDocumentStatus.PENDING
                || cancellationNote.getStatus() == ElectronicDocumentStatus.SENT) {
            result = actualizarEstado(cancellationNote.getId());
        } else {
            result = toResponse(cancellationNote);
        }

        if (result.status() != ElectronicDocumentStatus.ACCEPTED) {
            String detail = result.cdrMessage() == null || result.cdrMessage().isBlank()
                    ? "sin detalle del proveedor" : result.cdrMessage();
            throw new ReglaDeNegocioException(
                    "La nota de credito de anulacion no fue aceptada por el proveedor ("
                            + result.status().name() + "): " + detail);
        }
    }

    @Transactional
    public ElectronicDocumentResponse crearBorrador(
            Long saleId, CrearElectronicDocumentRequest request, String requestedIdempotencyKey, Long userId) {
        String idempotencyKey = normalizeIdempotencyKey(requestedIdempotencyKey, saleId, request);
        ElectronicDocument byKey = documentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (byKey != null) {
            return toResponse(byKey);
        }

        ElectronicDocument existingType = esNota(request.documentType()) ? null : documentRepository
                .findBySaleIdAndDocumentType(saleId, request.documentType()).orElse(null);
        if (existingType != null) {
            return toResponse(existingType);
        }

        CompanySettings company = companySettingsRepository.findById(TenantContext.getOrDefault())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Configuración de empresa", TenantContext.getOrDefault()));
        if (!company.isElectronicInvoicingEnabled()) {
            throw new OperacionNoPermitidaException("La facturación electrónica está desactivada para esta empresa");
        }

        BillingConfiguration billing = billingConfigurationRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new OperacionNoPermitidaException("La configuración de facturación no está disponible"));
        if (!billing.isEnabled() || billing.getCredentialsEncrypted() == null || billing.getCredentialsEncrypted().isBlank()) {
            throw new OperacionNoPermitidaException("El proveedor de facturación no está habilitado o no tiene credenciales configuradas");
        }

        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Venta", saleId));
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new ReglaDeNegocioException("Solo se puede facturar una venta completada");
        }
        if (sale.getCustomer() == null) {
            throw new ReglaDeNegocioException("La venta necesita un cliente para generar un comprobante electrónico");
        }
        if (request.documentType() == ElectronicDocumentType.FACTURA
                && (sale.getCustomer().getDocType() == null
                        || !"RUC".equals(sale.getCustomer().getDocType().name())
                        || sale.getCustomer().getDocNumber() == null
                        || !sale.getCustomer().getDocNumber().matches("\\d{11}"))) {
            throw new ReglaDeNegocioException("La factura requiere un cliente con RUC válido de 11 dígitos");
        }

        ElectronicDocument sourceDocument = esNota(request.documentType())
                ? validarNota(request, saleId)
                : null;

        String series = seriesFor(billing, request.documentType(), sourceDocument);
        if (series == null || series.isBlank()) {
            throw new OperacionNoPermitidaException(
                    "Configura la serie de " + request.documentType().name().toLowerCase() + " antes de emitir");
        }
        series = series.trim().toUpperCase();

        ElectronicDocument document = new ElectronicDocument();
        document.setSale(sale);
        document.setSourceDocument(sourceDocument);
        document.setProvider(billing.getProvider());
        document.setDocumentType(request.documentType());
        document.setStatus(ElectronicDocumentStatus.DRAFT);
        document.setSeries(series);
        // Verifac asigna el correlativo al recibir la serie; NubeFact exige que lo enviemos.
        document.setDocumentNumber(billing.getProvider()
                == com.freestyleperu.aplicacion.facturacion.domain.BillingProvider.NUBEFACT
                        ? siguienteNumero(series, request.documentType()) : null);
        document.setIdempotencyKey(idempotencyKey);
        List<SaleDetail> details = saleDetailRepository.findAllBySaleId(saleId);
        document.setAmount(importeDocumento(sale, details, request));
        document.setCurrencyCode(company.getCurrencyCode());
        document.setPayloadJson(snapshot(sale, details, company, series, sourceDocument, request));

        ElectronicDocument saved = documentRepository.save(document);
        auditService.log("COMPROBANTE_CREADO", "ELECTRONIC_DOCUMENT", saved.getId(), null,
                Map.of("saleId", saleId, "type", request.documentType().name(), "series", series), AuditResult.SUCCESS);
        return toResponse(saved);
    }

    @Transactional
    public ElectronicDocumentResponse enviar(Long documentId, Long userId) {
        ElectronicDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Comprobante electrónico", documentId));
        if (document.getStatus() != ElectronicDocumentStatus.DRAFT) {
            throw new ReglaDeNegocioException("Solo se puede enviar un comprobante que está en borrador");
        }

        CompanySettings company = companySettingsRepository.findById(TenantContext.getOrDefault())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Configuración de empresa", TenantContext.getOrDefault()));
        if (!company.isElectronicInvoicingEnabled()) {
            throw new OperacionNoPermitidaException("La facturación electrónica está desactivada para esta empresa");
        }
        BillingConfiguration billing = billingConfigurationRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new OperacionNoPermitidaException("La configuración de facturación no está disponible"));
        if (!billing.isEnabled() || billing.getCredentialsEncrypted() == null || billing.getCredentialsEncrypted().isBlank()) {
            throw new OperacionNoPermitidaException("El proveedor de facturación no está habilitado o no tiene credenciales configuradas");
        }

        Map<String, String> credentials = decryptCredentials(billing);
        document.setStatus(ElectronicDocumentStatus.PENDING);
        document.setSubmittedAt(java.time.LocalDateTime.now());
        documentRepository.saveAndFlush(document);
        ElectronicInvoicingProvider invoicingProvider = providerFor(document.getProvider());
        ElectronicInvoicingResult result;
        try {
            result = invoicingProvider.issue(
                    new ElectronicInvoicingCommand(document.getDocumentType(), document.getSeries(),
                            document.getDocumentNumber(), document.getPayloadJson()),
                    new BillingConfigurationData(billing.getEnvironment(), billing.getApiUrl(), credentials));
        } catch (ProveedorFacturacionException ex) {
            document.setStatus(ElectronicDocumentStatus.ERROR);
            document.setCdrCode("CONFIGURATION_OR_PROVIDER_ERROR");
            document.setCdrMessage(ex.getMessage());
            auditService.log("COMPROBANTE_ERROR", "ELECTRONIC_DOCUMENT", document.getId(), null,
                    Map.of("message", ex.getMessage()), AuditResult.FAILURE);
            return toResponse(documentRepository.save(document));
        }

        applyResult(document, result);
        ElectronicDocument saved = documentRepository.save(document);
        auditService.log(actionFor(result.status()), "ELECTRONIC_DOCUMENT", saved.getId(), null,
                Map.of("status", result.status().name(), "providerDocumentId",
                        result.providerDocumentId() == null ? "" : result.providerDocumentId()),
                result.status() == ElectronicDocumentStatus.ERROR || result.status() == ElectronicDocumentStatus.REJECTED
                        ? AuditResult.FAILURE : AuditResult.SUCCESS);
        return toResponse(saved);
    }

    @Transactional
    public ElectronicDocumentResponse actualizarEstado(Long documentId) {
        ElectronicDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Comprobante electrónico", documentId));
        if (document.getProviderDocumentId() == null || document.getProviderDocumentId().isBlank()) {
            throw new ReglaDeNegocioException("El comprobante todavía no tiene identificador externo");
        }
        ElectronicInvoicingProvider invoicingProvider = providerFor(document.getProvider());
        ElectronicInvoicingResult result = invoicingProvider.fetchStatus(
                document.getProviderDocumentId(), configurationData());
        applyResult(document, result);
        ElectronicDocument saved = documentRepository.save(document);
        auditService.log(actionFor(result.status()), "ELECTRONIC_DOCUMENT", saved.getId(), null,
                Map.of("status", result.status().name(), "source", "STATUS_POLL"),
                result.status() == ElectronicDocumentStatus.ERROR || result.status() == ElectronicDocumentStatus.REJECTED
                        ? AuditResult.FAILURE : AuditResult.SUCCESS);
        return toResponse(saved);
    }

    @Transactional
    public ElectronicDocumentResponse reintentar(Long documentId) {
        ElectronicDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Comprobante electrónico", documentId));
        if (document.getStatus() != ElectronicDocumentStatus.ERROR
                && document.getStatus() != ElectronicDocumentStatus.REJECTED) {
            throw new ReglaDeNegocioException("Solo se pueden reintentar comprobantes con error o rechazados");
        }
        if (document.getProviderDocumentId() == null || document.getProviderDocumentId().isBlank()) {
            throw new ReglaDeNegocioException(
                    "Este comprobante no tiene identificador externo; requiere conciliación manual antes de reintentar");
        }
        try {
            ElectronicInvoicingProvider invoicingProvider = providerFor(document.getProvider());
            ElectronicInvoicingResult result = invoicingProvider.retry(
                    new ElectronicInvoicingCommand(document.getDocumentType(), document.getSeries(),
                            document.getDocumentNumber(), document.getPayloadJson()),
                    document.getProviderDocumentId(), configurationData());
            applyResult(document, result);
            ElectronicDocument saved = documentRepository.save(document);
            auditService.log("COMPROBANTE_REINTENTADO", "ELECTRONIC_DOCUMENT", saved.getId(), null,
                    Map.of("status", result.status().name()), AuditResult.SUCCESS);
            return toResponse(saved);
        } catch (ProveedorFacturacionException ex) {
            document.setStatus(ElectronicDocumentStatus.ERROR);
            document.setCdrCode("RETRY_FAILED");
            document.setCdrMessage(ex.getMessage());
            auditService.log("COMPROBANTE_ERROR", "ELECTRONIC_DOCUMENT", document.getId(), null,
                    Map.of("message", ex.getMessage(), "source", "RETRY"), AuditResult.FAILURE);
            return toResponse(documentRepository.save(document));
        }
    }

    public ElectronicInvoicingResource descargar(Long documentId, String resource) {
        ElectronicDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Comprobante electrónico", documentId));
        if (document.getProviderDocumentId() == null || document.getProviderDocumentId().isBlank()) {
            throw new ReglaDeNegocioException("El comprobante todavía no tiene identificador externo");
        }
        return providerFor(document.getProvider()).download(document.getProviderDocumentId(), resource, configurationData());
    }

    private BillingConfigurationData configurationData() {
        BillingConfiguration billing = billingConfigurationRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new OperacionNoPermitidaException("La configuración de facturación no está disponible"));
        if (!billing.isEnabled() || billing.getCredentialsEncrypted() == null || billing.getCredentialsEncrypted().isBlank()) {
            throw new OperacionNoPermitidaException("El proveedor de facturación no está habilitado o no tiene credenciales configuradas");
        }
        return new BillingConfigurationData(billing.getEnvironment(), billing.getApiUrl(), decryptCredentials(billing));
    }

    private Map<String, String> decryptCredentials(BillingConfiguration billing) {
        try {
            return objectMapper.readValue(encryptionService.decrypt(billing.getCredentialsEncrypted()), Map.class);
        } catch (Exception ex) {
            throw new OperacionNoPermitidaException("No se pudieron leer las credenciales del proveedor de facturación");
        }
    }

    private void applyResult(ElectronicDocument document, ElectronicInvoicingResult result) {
        document.setStatus(result.status());
        document.setProviderDocumentId(result.providerDocumentId());
        document.setProviderStatus(result.providerStatus());
        if (result.providerSeries() != null && !result.providerSeries().isBlank()) {
            document.setSeries(result.providerSeries());
        }
        if (result.providerNumber() != null && !result.providerNumber().isBlank()) {
            document.setDocumentNumber(result.providerNumber());
        }
        document.setCdrCode(result.cdrCode());
        document.setCdrMessage(result.cdrMessage());
        document.setXmlUrl(result.xmlUrl());
        document.setCdrUrl(result.cdrUrl());
        if (result.status() == ElectronicDocumentStatus.ACCEPTED) {
            document.setAcceptedAt(java.time.LocalDateTime.now());
        }
        if (result.status() == ElectronicDocumentStatus.REJECTED) {
            document.setRejectedAt(java.time.LocalDateTime.now());
        }
    }

    private String actionFor(ElectronicDocumentStatus status) {
        return switch (status) {
            case ACCEPTED -> "COMPROBANTE_ACEPTADO";
            case REJECTED -> "COMPROBANTE_RECHAZADO";
            case ERROR -> "COMPROBANTE_ERROR";
            default -> "COMPROBANTE_ENVIADO";
        };
    }

    private String seriesFor(BillingConfiguration config, ElectronicDocumentType type, ElectronicDocument sourceDocument) {
        if (config.getProvider() == com.freestyleperu.aplicacion.facturacion.domain.BillingProvider.NUBEFACT
                && esNota(type) && sourceDocument != null) {
            return sourceDocument.getSeries();
        }
        return switch (type) {
            case FACTURA -> config.getInvoiceSeries();
            case BOLETA -> config.getReceiptSeries();
            case NOTA_CREDITO -> config.getCreditNoteSeries();
            case NOTA_DEBITO -> config.getDebitNoteSeries();
        };
    }

    private ElectronicInvoicingProvider providerFor(
            com.freestyleperu.aplicacion.facturacion.domain.BillingProvider provider) {
        ElectronicInvoicingProvider result = invoicingProviders.get(provider);
        if (result == null) {
            throw new OperacionNoPermitidaException("El proveedor de facturaciÃ³n " + provider + " no estÃ¡ disponible");
        }
        return result;
    }

    private String siguienteNumero(String series, ElectronicDocumentType type) {
        int max = documentRepository.findAll().stream()
                .filter(document -> document.getDocumentType() == type)
                .filter(document -> series.equalsIgnoreCase(document.getSeries()))
                .map(ElectronicDocument::getDocumentNumber)
                .filter(value -> value != null && value.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return String.valueOf(max + 1);
    }

    private boolean esNota(ElectronicDocumentType type) {
        return type == ElectronicDocumentType.NOTA_CREDITO || type == ElectronicDocumentType.NOTA_DEBITO;
    }

    private ElectronicDocument validarNota(CrearElectronicDocumentRequest request, Long saleId) {
        if (request.sourceDocumentId() == null) {
            throw new ReglaDeNegocioException("La nota requiere seleccionar el comprobante de origen");
        }
        ElectronicDocument source = documentRepository.findById(request.sourceDocumentId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Comprobante de origen", request.sourceDocumentId()));
        if (source.getSale() == null || !saleId.equals(source.getSale().getId())) {
            throw new ReglaDeNegocioException("El comprobante de origen no pertenece a esta venta");
        }
        if (source.getDocumentType() != ElectronicDocumentType.BOLETA
                && source.getDocumentType() != ElectronicDocumentType.FACTURA) {
            throw new ReglaDeNegocioException("Una nota solo puede modificar una boleta o factura");
        }
        if (source.getStatus() != ElectronicDocumentStatus.ACCEPTED
                || source.getProviderDocumentId() == null || source.getProviderDocumentId().isBlank()
                || source.getDocumentNumber() == null || source.getDocumentNumber().isBlank()) {
            throw new ReglaDeNegocioException("El comprobante de origen debe estar aceptado por su proveedor de facturación");
        }
        String code = request.reasonCode() == null ? "" : request.reasonCode().trim();
        String description = request.reasonDescription() == null ? "" : request.reasonDescription().trim();
        boolean validCode = request.documentType() == ElectronicDocumentType.NOTA_CREDITO
                ? code.matches("0[1-9]|1[0-3]")
                : code.matches("0[1-3]");
        if (!validCode) {
            throw new ReglaDeNegocioException("El código de motivo no corresponde al tipo de nota");
        }
        if (description.isBlank()) {
            throw new ReglaDeNegocioException("La nota requiere describir el motivo fiscal");
        }
        if ("07".equals(code) && (request.items() == null || request.items().isEmpty())) {
            throw new ReglaDeNegocioException("La devolucion por item requiere seleccionar al menos un producto");
        }
        validarItemsDeNota(request, saleId);
        return source;
    }

    private void validarItemsDeNota(CrearElectronicDocumentRequest request, Long saleId) {
        if (request.items() == null || request.items().isEmpty()) {
            return;
        }
        Map<Long, SaleDetail> detailsByVariant = new LinkedHashMap<>();
        for (SaleDetail detail : saleDetailRepository.findAllBySaleId(saleId)) {
            if (detailsByVariant.put(detail.getVariant().getId(), detail) != null) {
                throw new ReglaDeNegocioException(
                        "La venta contiene el mismo producto mas de una vez; no se puede seleccionar por producto");
            }
        }
        Map<Long, Boolean> requested = new LinkedHashMap<>();
        for (var item : request.items()) {
            if (requested.put(item.variantId(), Boolean.TRUE) != null) {
                throw new ReglaDeNegocioException("No se puede repetir un producto en la nota");
            }
            SaleDetail detail = detailsByVariant.get(item.variantId());
            if (detail == null) {
                throw new ReglaDeNegocioException("El producto seleccionado no pertenece a la venta");
            }
            if (item.quantity() > detail.getQuantity()) {
                throw new ReglaDeNegocioException(
                        "La cantidad de la nota supera la cantidad vendida para " + detail.getProductName());
            }
        }
    }

    private String normalizeIdempotencyKey(
            String requested, Long saleId, CrearElectronicDocumentRequest request) {
        ElectronicDocumentType type = request.documentType();
        String value = requested == null || requested.isBlank()
                ? "sale-" + saleId + "-" + type.name()
                        + (esNota(type) ? "-source-" + request.sourceDocumentId() + "-reason-" + request.reasonCode()
                                + (request.items() == null || request.items().isEmpty()
                                        ? "" : "-items-" + Integer.toHexString(request.items().hashCode())) : "")
                : requested.trim();
        if (value.length() > 100) {
            throw new ReglaDeNegocioException("La clave de idempotencia no puede superar 100 caracteres");
        }
        return value;
    }

    private String snapshot(
            Sale sale, List<SaleDetail> details, CompanySettings company, String series,
            ElectronicDocument sourceDocument, CrearElectronicDocumentRequest request) {
        Map<Long, Integer> requestedQuantities = cantidadesSolicitadas(request);
        boolean partial = esNota(request.documentType()) && !requestedQuantities.isEmpty();
        BigDecimal documentSubtotal = partial ? BigDecimal.ZERO : sale.getSubtotal();
        BigDecimal documentDiscount = partial ? BigDecimal.ZERO : sale.getDiscountAmount();
        BigDecimal documentTotal = partial ? BigDecimal.ZERO : sale.getTotal();
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("saleId", sale.getId());
        root.put("saleNumber", sale.getSaleNumber());
        root.put("document", Map.of("series", series));
        root.put("issuer", Map.of(
                "ruc", valueOrEmpty(company.getRuc()),
                "name", valueOrEmpty(company.getName()),
                "address", valueOrEmpty(company.getAddress())));
        Map<String, Object> customer = new LinkedHashMap<>();
        customer.put("fullName", sale.getCustomer().getFullName());
        customer.put("docType", sale.getCustomer().getDocType().name());
        customer.put("docNumber", sale.getCustomer().getDocNumber());
        customer.put("email", sale.getCustomer().getEmail());
        root.put("customer", customer);
        root.put("subtotal", documentSubtotal);
        root.put("discountAmount", documentDiscount);
        root.put("shippingAmount", partial ? BigDecimal.ZERO : sale.getShippingAmount());
        root.put("total", documentTotal);
        root.put("currencyCode", company.getCurrencyCode());
        root.put("igvRate", company.getIgvRate());
        List<Map<String, Object>> lines = new ArrayList<>();
        for (SaleDetail detail : details) {
            Integer requestedQuantity = requestedQuantities.get(detail.getVariant().getId());
            if (partial && requestedQuantity == null) {
                continue;
            }
            int quantity = requestedQuantity == null ? detail.getQuantity() : requestedQuantity;
            BigDecimal factor = BigDecimal.valueOf(quantity)
                    .divide(BigDecimal.valueOf(detail.getQuantity()), 10, RoundingMode.HALF_UP);
            BigDecimal lineDiscount = partial
                    ? detail.getDiscountAmount().multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    : detail.getDiscountAmount();
            BigDecimal lineGross = detail.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
            BigDecimal lineSubtotal = lineGross.subtract(lineDiscount).setScale(2, RoundingMode.HALF_UP);
            if (partial) {
                documentSubtotal = documentSubtotal.add(lineGross);
                documentDiscount = documentDiscount.add(lineDiscount);
                documentTotal = documentTotal.add(lineSubtotal);
            }
            lines.add(new LinkedHashMap<>(Map.of(
                    "sku", detail.getVariantSku(),
                    "description", detail.getProductName(),
                    "quantity", quantity,
                    "unitPrice", detail.getUnitPrice(),
                    "discountAmount", lineDiscount,
                    "subtotal", lineSubtotal)));
        }
        if (partial) {
            root.put("subtotal", documentSubtotal);
            root.put("discountAmount", documentDiscount);
            root.put("total", documentTotal);
        }
        root.put("lines", lines);
        if (sourceDocument != null) {
            root.put("note", Map.of(
                    "sourceDocumentType", sourceDocument.getDocumentType().name(),
                    "sourceDocumentTypeCode", sourceDocument.getDocumentType() == ElectronicDocumentType.FACTURA ? "01" : "03",
                    "sourceSeries", sourceDocument.getSeries(),
                    "sourceNumber", sourceDocument.getDocumentNumber(),
                    "sourceSeriesNumber", sourceDocument.getSeries() + "-" + sourceDocument.getDocumentNumber(),
                    "reasonCode", request.reasonCode().trim(),
                    "reasonDescription", request.reasonDescription().trim()));
        }
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo construir el snapshot del comprobante", ex);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal importeDocumento(
            Sale sale, List<SaleDetail> details, CrearElectronicDocumentRequest request) {
        Map<Long, Integer> requestedQuantities = cantidadesSolicitadas(request);
        if (requestedQuantities.isEmpty()) {
            return sale.getTotal();
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SaleDetail detail : details) {
            Integer quantity = requestedQuantities.get(detail.getVariant().getId());
            if (quantity == null) {
                continue;
            }
            BigDecimal factor = BigDecimal.valueOf(quantity)
                    .divide(BigDecimal.valueOf(detail.getQuantity()), 10, RoundingMode.HALF_UP);
            BigDecimal discount = detail.getDiscountAmount().multiply(factor).setScale(2, RoundingMode.HALF_UP);
            total = total.add(detail.getUnitPrice().multiply(BigDecimal.valueOf(quantity)).subtract(discount));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, Integer> cantidadesSolicitadas(CrearElectronicDocumentRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (var item : request.items()) {
            result.put(item.variantId(), item.quantity());
        }
        return result;
    }

    private ElectronicDocumentResponse toResponse(ElectronicDocument document) {
        return new ElectronicDocumentResponse(
                document.getId(), document.getSale().getId(), document.getSale().getSaleNumber(),
                document.getSourceDocument() == null ? null : document.getSourceDocument().getId(), document.getProvider(),
                document.getDocumentType(), document.getStatus(), document.getSeries(), document.getDocumentNumber(),
                document.getAmount(), document.getCurrencyCode(), document.getProviderDocumentId(),
                document.getProviderStatus(), reason(document, "reasonCode"), reason(document, "reasonDescription"),
                document.getCdrCode(), document.getCdrMessage(), document.getPdfUrl(),
                document.getXmlUrl(), document.getCdrUrl(), document.getSubmittedAt(), document.getAcceptedAt(),
                document.getRejectedAt(), document.getCreatedAt());
    }

    @SuppressWarnings("unchecked")
    private String reason(ElectronicDocument document, String key) {
        try {
            Map<String, Object> root = objectMapper.readValue(document.getPayloadJson(), Map.class);
            Map<String, Object> note = root.get("note") instanceof Map<?, ?> value
                    ? (Map<String, Object>) value : Map.of();
            Object result = note.get(key);
            return result == null ? null : String.valueOf(result);
        } catch (Exception ex) {
            return null;
        }
    }
}
