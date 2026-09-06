package com.freestyleperu.aplicacion.facturacion.provider;

import com.freestyleperu.aplicacion.facturacion.domain.BillingProvider;
import com.freestyleperu.aplicacion.facturacion.domain.BillingProviderEnvironment;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocumentStatus;
import com.freestyleperu.aplicacion.facturacion.exception.ProveedorFacturacionException;
import com.freestyleperu.aplicacion.facturacion.port.BillingConfigurationData;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingCommand;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingProvider;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingResource;
import com.freestyleperu.aplicacion.facturacion.port.ElectronicInvoicingResult;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

/**
 * Adaptador de Qynex CPE, el motor propio de emision a SUNAT.
 *
 * <p>No sustituye a NubeFact ni a Verifac: es una tercera opcion para las empresas que no
 * traen proveedor propio. La empresa aporta su certificado y su Clave SOL en el panel de
 * CPE, asi que Qynex opera la infraestructura sin firmar ante SUNAT con credenciales
 * propias — por eso el modelo no es de PSE ni de OSE.
 *
 * <p>Construido contra {@code docs/integracion-consumidores.md} y el OpenAPI publicado en
 * {@code /api-docs}. Tres cosas de ese contrato condicionan este codigo:
 *
 * <ul>
 *   <li><b>La emision responde 202, no 200.</b> CPE encola y todavia no fue a SUNAT, asi que
 *       aqui nunca se devuelve ACCEPTED en la emision: el documento queda en la cola de
 *       reconciliacion.
 *   <li><b>{@code FAILED} no es un error definitivo.</b> Significa que SUNAT no respondio y
 *       que CPE reintenta por su cuenta. Se mapea a SENT para seguir consultando; tratarlo
 *       como error terminal llevaria a emitir un segundo comprobante por la misma venta.
 *   <li><b>Un campo que no existe se rechaza con 400.</b> CPE no ignora los desconocidos a
 *       proposito, porque escribir {@code plasticBags} en vez de {@code plasticBag} devolvia
 *       un 202 y una factura sin el impuesto a la bolsa. Este adaptador manda exactamente
 *       los campos del contrato y ninguno mas.
 * </ul>
 */
@Component
public class QynexCpeProvider implements ElectronicInvoicingProvider {

    private final ObjectMapper objectMapper;

    public QynexCpeProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public BillingProvider type() {
        return BillingProvider.QYNEX_CPE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ElectronicInvoicingResult issue(
            ElectronicInvoicingCommand command, BillingConfigurationData configuration) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            // Sin ella, un timeout seguido de un reintento emitiria dos comprobantes por una
            // sola venta, y el segundo solo se corrige con una nota de credito ante SUNAT.
            throw new ProveedorFacturacionException(
                    "Falta la clave de idempotencia: Qynex CPE la exige en toda emision");
        }
        Map<String, Object> snapshot = parse(command.payloadJson());
        Map<String, Object> request = buildRequest(command, snapshot);
        String path = switch (command.documentType()) {
            case FACTURA -> "/api/v1/invoices";
            case BOLETA -> "/api/v1/boletas";
            case NOTA_CREDITO -> "/api/v1/credit-notes";
            case NOTA_DEBITO -> "/api/v1/debit-notes";
        };
        try {
            ResponseEntity<Map> response = client(configuration)
                    .post()
                    .uri(path)
                    .header("Idempotency-Key", command.idempotencyKey())
                    .body(request)
                    .retrieve()
                    .toEntity(Map.class);
            return parseDocument(response.getBody());
        } catch (RestClientResponseException ex) {
            return errorDe(ex);
        } catch (RestClientException ex) {
            return new ElectronicInvoicingResult(
                    ElectronicDocumentStatus.ERROR, null, null, null, null, "PROVIDER_REQUEST_FAILED",
                    "No se pudo comunicar con Qynex CPE en este momento", null, null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public ElectronicInvoicingResult fetchStatus(String providerDocumentId, BillingConfigurationData configuration) {
        try {
            ResponseEntity<Map> response = client(configuration)
                    .get()
                    .uri("/api/v1/documents/{id}", providerDocumentId)
                    .retrieve()
                    .toEntity(Map.class);
            return parseDocument(response.getBody());
        } catch (RestClientResponseException ex) {
            throw new ProveedorFacturacionException("Qynex CPE no pudo consultar el estado del comprobante");
        } catch (RestClientException ex) {
            throw new ProveedorFacturacionException("No se pudo consultar el estado en Qynex CPE");
        }
    }

    /**
     * Reintenta un envio que fallo tecnicamente, conservando el numero. No sirve para un
     * rechazado —ahi el correlativo esta quemado y hay que emitir uno nuevo— y CPE responde
     * 422 explicandolo.
     */
    @Override
    @SuppressWarnings("unchecked")
    public ElectronicInvoicingResult retry(String providerDocumentId, BillingConfigurationData configuration) {
        try {
            ResponseEntity<Map> response = client(configuration)
                    .post()
                    .uri("/api/v1/documents/{id}/retry", providerDocumentId)
                    .header("Idempotency-Key", "retry-" + providerDocumentId)
                    .retrieve()
                    .toEntity(Map.class);
            return parseDocument(response.getBody());
        } catch (RestClientResponseException ex) {
            throw new ProveedorFacturacionException(mensajeDe(ex));
        } catch (RestClientException ex) {
            throw new ProveedorFacturacionException("No se pudo reenviar el comprobante a Qynex CPE");
        }
    }

    @Override
    public ElectronicInvoicingResource download(
            String providerDocumentId, String resource, BillingConfigurationData configuration) {
        String path = switch (resource) {
            case "pdf" -> "/api/v1/documents/{id}/pdf";
            case "xml" -> "/api/v1/documents/{id}/xml";
            case "cdr" -> "/api/v1/documents/{id}/cdr";
            default -> throw new ProveedorFacturacionException("Recurso de comprobante no soportado");
        };
        try {
            ResponseEntity<byte[]> response = client(configuration)
                    .get()
                    .uri(path, providerDocumentId)
                    .retrieve()
                    .toEntity(byte[].class);
            byte[] content = response.getBody();
            if (content == null || content.length == 0) {
                throw new ProveedorFacturacionException("Qynex CPE no devolvio el recurso solicitado");
            }
            // Comprobado contra CPE: el CDR viaja como ZIP (es como lo entrega SUNAT), no
            // como XML suelto. Etiquetarlo mal haria que el navegador intentara mostrarlo.
            String tipo = switch (resource) {
                case "pdf" -> "application/pdf";
                case "xml" -> "application/xml";
                case "cdr" -> "application/zip";
                default -> "application/octet-stream";
            };
            String extension = "cdr".equals(resource) ? "zip" : resource;
            return new ElectronicInvoicingResource(content, tipo, providerDocumentId + "." + extension);
        } catch (RestClientResponseException ex) {
            throw new ProveedorFacturacionException("Qynex CPE no pudo entregar el comprobante solicitado");
        } catch (RestClientException ex) {
            throw new ProveedorFacturacionException("No se pudo descargar el comprobante de Qynex CPE");
        }
    }

    /**
     * Series que esta credencial puede usar. CPE ya filtra por empresa y por reserva, asi
     * que lo que llega aqui es exactamente lo que se puede elegir sin que falle al emitir.
     *
     * <p>Si la consulta falla se devuelve vacio en vez de propagar: esto alimenta un
     * desplegable de una pantalla de configuracion, y que CPE no responda no puede impedir
     * abrirla ni guardar el resto de los ajustes.
     */
    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<SerieDisponible> series(BillingConfigurationData configuration) {
        try {
            ResponseEntity<Map> response = client(configuration)
                    .get()
                    .uri("/api/v1/series")
                    .retrieve()
                    .toEntity(Map.class);
            Object datos = response.getBody() == null ? null : response.getBody().get("data");
            if (!(datos instanceof List<?> lista)) {
                return java.util.List.of();
            }
            List<SerieDisponible> series = new ArrayList<>();
            for (Object cruda : lista) {
                Map<String, Object> fila = mapa(cruda);
                String serie = texto(fila.get("series"), null);
                if (serie == null) continue;
                series.add(new SerieDisponible(
                        texto(fila.get("documentType"), ""),
                        texto(fila.get("documentTypeName"), ""),
                        serie));
            }
            return series;
        } catch (RestClientException | ProveedorFacturacionException ex) {
            return java.util.List.of();
        }
    }

    // --- traduccion del snapshot al contrato de CPE -------------------------------------

    private Map<String, Object> buildRequest(ElectronicInvoicingCommand command, Map<String, Object> snapshot) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("series", command.series());
        request.put("currency", texto(snapshot.get("currencyCode"), "PEN"));
        request.put("operationType", "0101");
        request.put("paymentMeans", "Contado");
        request.put("externalReference", texto(snapshot.get("saleNumber"), null));
        request.put("customer", cliente(snapshot));
        request.put("items", lineas(snapshot));
        // CPE calcula para declarar y nosotros para cobrar: mandando el total se comparan al
        // centimo y un descuadre sale como 422 sin consumir correlativo, en vez de acabar
        // cobrando una cifra y declarando otra.
        BigDecimal total = numero(snapshot.get("total"));
        if (total != null) {
            request.put("expectedTotal", total);
        }
        return request;
    }

    private Map<String, Object> cliente(Map<String, Object> snapshot) {
        Map<String, Object> origen = mapa(snapshot.get("customer"));
        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("documentType", tipoDocumento(texto(origen.get("docType"), "SIN_DOCUMENTO")));
        cliente.put("documentNumber", texto(origen.get("docNumber"), "00000000"));
        cliente.put("name", texto(origen.get("fullName"), "CLIENTE"));
        return cliente;
    }

    /** Catalogo 06 de SUNAT. Una boleta a consumidor final va con tipo 0 y ceros. */
    private String tipoDocumento(String docType) {
        return switch (docType == null ? "" : docType.toUpperCase()) {
            case "RUC" -> "6";
            case "DNI" -> "1";
            case "CE", "CARNET_EXTRANJERIA" -> "4";
            case "PASAPORTE" -> "7";
            default -> "0";
        };
    }

    private List<Map<String, Object>> lineas(Map<String, Object> snapshot) {
        List<Map<String, Object>> items = new ArrayList<>();
        Object crudas = snapshot.get("lines");
        if (!(crudas instanceof List<?> lista)) {
            return items;
        }
        for (Object cruda : lista) {
            Map<String, Object> linea = mapa(cruda);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", texto(linea.get("sku"), null));
            item.put("description", texto(linea.get("description"), "PRODUCTO"));
            item.put("quantity", numero(linea.get("quantity")));
            item.put("unitPrice", numero(linea.get("unitPrice")));
            // El maestro de Shop guarda el precio con IGV incluido, que es como piensa una
            // tienda. CPE asume lo mismo por defecto, pero se manda explicito.
            item.put("priceIncludesIgv", Boolean.TRUE);
            item.put("unitCode", texto(linea.get("unitCode"), "NIU"));
            item.put("igvAffectationType", texto(linea.get("igvAffectationType"), "10"));
            String codigoSunat = texto(linea.get("sunatProductCode"), null);
            if (codigoSunat != null) {
                // Solo si el producto esta clasificado: mandar uno inventado declararia una
                // clasificacion falsa, y desde el 01.01.2027 ademas quemaria el correlativo.
                item.put("sunatProductCode", codigoSunat);
            }
            items.add(item);
        }
        return items;
    }

    // --- lectura de la respuesta --------------------------------------------------------

    private ElectronicInvoicingResult parseDocument(Map<String, Object> body) {
        Map<String, Object> data = mapa(body == null ? null : body.get("data"));
        if (data.isEmpty()) {
            throw new ProveedorFacturacionException("Qynex CPE devolvio una respuesta sin datos del comprobante");
        }
        Map<String, Object> sunat = mapa(data.get("sunat"));
        String estadoCpe = texto(data.get("status"), "QUEUED");
        Object numero = data.get("number");
        return new ElectronicInvoicingResult(
                estadoDe(estadoCpe),
                texto(data.get("id"), null),
                estadoCpe,
                texto(data.get("series"), null),
                numero == null ? null : String.valueOf(numero),
                texto(sunat.get("code"), null),
                texto(sunat.get("description"), null),
                null,
                null);
    }

    /**
     * Estados de CPE traducidos a los de Shop.
     *
     * <p>{@code FAILED} se mapea a SENT y no a ERROR a proposito: en CPE significa que SUNAT
     * no llego a responder y que la cola reintenta sola. Marcarlo como error haria que Shop
     * lo diera por terminado y alguien acabaria emitiendo un segundo comprobante por la misma
     * venta. SENT lo deja en la cola de reconciliacion, que es lo correcto.
     */
    private ElectronicDocumentStatus estadoDe(String estadoCpe) {
        return switch (estadoCpe == null ? "" : estadoCpe.toUpperCase()) {
            // SunatStatus: lo que respondio SUNAT. Son los unicos estados terminales.
            case "ACCEPTED" -> ElectronicDocumentStatus.ACCEPTED;
            // Aceptado con observaciones sigue siendo aceptado: el comprobante es valido y
            // no hay nada que reemitir. Tratarlo como pendiente lo dejaria sondeando para
            // siempre un documento que ya termino su ciclo.
            case "ACCEPTED_WITH_OBSERVATIONS" -> ElectronicDocumentStatus.ACCEPTED;
            case "REJECTED" -> ElectronicDocumentStatus.REJECTED;
            // InternalStatus: todavia no hay respuesta de SUNAT.
            case "DRAFT", "VALIDATING", "READY", "QUEUED" -> ElectronicDocumentStatus.PENDING;
            case "SIGNING", "SIGNED", "SENDING", "COMPLETED" -> ElectronicDocumentStatus.SENT;
            // FAILED no es terminal: SUNAT no llego a responder y la cola de CPE reintenta
            // sola. Se deja en SENT para seguir consultando; darlo por terminado acabaria
            // con alguien emitiendo un segundo comprobante por la misma venta.
            case "FAILED" -> ElectronicDocumentStatus.SENT;
            // Un estado que no conocemos se trata como en vuelo, nunca como terminal: es el
            // lado seguro si CPE incorpora uno nuevo.
            default -> ElectronicDocumentStatus.SENT;
        };
    }

    /**
     * Un 4xx de CPE es validacion: no llego a SUNAT y no se consumio correlativo, asi que el
     * documento queda en ERROR y se puede corregir y reenviar. Solo SUNAT puede rechazar de
     * verdad, y eso llega por el estado del documento, no por el codigo HTTP.
     */
    private ElectronicInvoicingResult errorDe(RestClientResponseException ex) {
        return new ElectronicInvoicingResult(
                ElectronicDocumentStatus.ERROR, null, null, null, null,
                String.valueOf(ex.getStatusCode().value()), mensajeDe(ex), null, null);
    }

    /** CPE devuelve error.message y un requestId que hay que conservar para rastrearlo. */
    @SuppressWarnings("unchecked")
    private String mensajeDe(RestClientResponseException ex) {
        try {
            Map<String, Object> body = objectMapper.readValue(ex.getResponseBodyAsString(), Map.class);
            Map<String, Object> error = mapa(body.get("error"));
            String mensaje = texto(error.get("message"), "Qynex CPE rechazo la peticion");
            String requestId = texto(body.get("requestId"), null);
            return requestId == null ? mensaje : mensaje + " (requestId " + requestId + ")";
        } catch (RuntimeException ignored) {
            return "Qynex CPE rechazo la peticion";
        }
    }

    // --- infraestructura ----------------------------------------------------------------

    private RestClient client(BillingConfigurationData configuration) {
        String apiKey = credencial(configuration, "apiKey");
        String apiSecret = credencial(configuration, "apiSecret");
        if (apiKey == null || apiSecret == null) {
            throw new ProveedorFacturacionException(
                    "Faltan la API key y el secret de Qynex CPE en la configuracion de la empresa");
        }
        String basic = Base64.getEncoder().encodeToString(
                (apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .baseUrl(apiUrl(configuration))
                .defaultHeader("Authorization", "Basic " + basic)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Sin valor por defecto a proposito. Donde vive CPE depende del despliegue: desde el
     * contenedor de Shop, "localhost" es el propio contenedor y no CPE, asi que un default
     * que parece razonable fallaria en silencio justo donde importa. Se exige explicito.
     */
    private String apiUrl(BillingConfigurationData configuration) {
        if (configuration.apiUrl() == null || configuration.apiUrl().isBlank()) {
            throw new ProveedorFacturacionException(
                    "Falta la URL de Qynex CPE en la configuracion de la empresa");
        }
        String url = configuration.apiUrl().trim();
        if (configuration.environment() == BillingProviderEnvironment.PRODUCTION && !url.startsWith("https://")) {
            throw new ProveedorFacturacionException("La URL de Qynex CPE debe usar HTTPS en produccion");
        }
        return url;
    }

    private String credencial(BillingConfigurationData configuration, String clave) {
        String valor = configuration.credentials().get(clave);
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (RuntimeException ex) {
            throw new ProveedorFacturacionException("El detalle del comprobante no se pudo interpretar");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapa(Object valor) {
        return valor instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private String texto(Object valor, String porDefecto) {
        if (valor == null) return porDefecto;
        String s = String.valueOf(valor).trim();
        return s.isEmpty() ? porDefecto : s;
    }

    private BigDecimal numero(Object valor) {
        if (valor == null) return null;
        if (valor instanceof BigDecimal b) return b;
        if (valor instanceof Number n) return new BigDecimal(n.toString());
        try {
            return new BigDecimal(String.valueOf(valor).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
