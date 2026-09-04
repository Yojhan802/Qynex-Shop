package com.freestyleperu.aplicacion.reclamo.web;

import com.freestyleperu.aplicacion.reclamo.dto.request.CreateComplaintRequest;
import com.freestyleperu.aplicacion.reclamo.dto.request.RespondComplaintRequest;
import com.freestyleperu.aplicacion.reclamo.dto.response.ComplaintReceiptResponse;
import com.freestyleperu.aplicacion.reclamo.dto.response.ComplaintResponse;
import com.freestyleperu.aplicacion.reclamo.dto.response.PublicComplaintResponse;
import com.freestyleperu.aplicacion.reclamo.service.ComplaintBookService;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.shared.security.JwtService;
import com.freestyleperu.aplicacion.shared.security.TenantContext;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ComplaintBookController {

    private final ComplaintBookService service;
    private final JwtService jwtService;

    public ComplaintBookController(ComplaintBookService service, JwtService jwtService) {
        this.service = service;
        this.jwtService = jwtService;
    }

    /**
     * Devuelve la constancia completa (D.S. 011-2011-PCM, Art. 5) porque es la única
     * respuesta dirigida a quien acaba de declarar esos datos. La consulta posterior
     * por número usa {@link #get} y nunca expone datos personales.
     */
    @PostMapping("/api/store/complaints")
    public ResponseEntity<ComplaintReceiptResponse> create(@Valid @RequestBody CreateComplaintRequest request) {
        ComplaintReceiptResponse receipt = service.createAndIssueReceipt(request);
        return ResponseEntity.created(URI.create("/api/store/complaints/" + receipt.entryNumber())).body(receipt);
    }

    @GetMapping("/api/store/complaints/{entryNumber}")
    public PublicComplaintResponse get(@PathVariable String entryNumber) { return service.getPublicByNumber(entryNumber); }

    /**
     * Constancia en PDF para el consumidor que acaba de registrar la hoja.
     *
     * <p>Deliberadamente NO se identifica por número de hoja: los correlativos son
     * predecibles, así que un endpoint por número dejaría recorrerlos y descargar los datos
     * personales de todos los consumidores. El token va en la respuesta al registro, vive
     * 15 minutos y solo sirve para esa hoja y esa tienda.
     *
     * <p>Es la vía de entrega efectiva mientras no haya SMTP configurado: sin ella el
     * consumidor no se llevaría ninguna copia.
     */
    @GetMapping("/api/store/complaints/receipt.pdf")
    public ResponseEntity<byte[]> descargarConstancia(
            @RequestParam(name = "token", required = false) String token) {
        // El parámetro se declara opcional para que su ausencia caiga en el mismo 404 que un
        // token inválido, en vez de en el 500 de un parámetro obligatorio que falta.
        Long complaintId = token == null || token.isBlank()
                ? null
                : jwtService.parseComplaintReceiptToken(token, TenantContext.getOrDefault());
        if (complaintId == null) {
            // Mismo error que una hoja inexistente: no se distingue "token vencido" de
            // "hoja que no existe", para no confirmar qué números están ocupados.
            throw RecursoNoEncontradoException.de("Constancia", 0L);
        }
        return pdf(service.constanciaPdfDe(complaintId), service.nombrePdfDe(complaintId));
    }

    /** La empresa reimprime la constancia de cualquiera de sus hojas, desde su panel. */
    @GetMapping("/api/complaints/{id}/receipt.pdf")
    @PreAuthorize("hasAuthority('" + Permisos.RECLAMOS_CONSULTAR + "')")
    public ResponseEntity<byte[]> descargarConstanciaStaff(@PathVariable Long id) {
        return pdf(service.constanciaPdfDe(id), service.nombrePdfDe(id));
    }

    private ResponseEntity<byte[]> pdf(byte[] contenido, String nombre) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(contenido.length);
        headers.setContentDisposition(ContentDisposition.attachment().filename(nombre).build());
        // Lleva datos personales de una sola persona: fuera de cachés compartidas.
        headers.setCacheControl("private, no-store");
        return new ResponseEntity<>(contenido, headers, 200);
    }

    @GetMapping("/api/complaints")
    @PreAuthorize("hasAuthority('" + Permisos.RECLAMOS_CONSULTAR + "')")
    public PageResponse<ComplaintResponse> list(Pageable pageable) { return service.list(pageable); }

    @PatchMapping("/api/complaints/{id}/response")
    @PreAuthorize("hasAuthority('" + Permisos.RECLAMOS_RESPONDER + "')")
    public ComplaintResponse respond(@PathVariable Long id, @Valid @RequestBody RespondComplaintRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return service.respond(id, request, currentUser.id());
    }
}
