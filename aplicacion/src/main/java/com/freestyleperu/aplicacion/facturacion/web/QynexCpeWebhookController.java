package com.freestyleperu.aplicacion.facturacion.web;

import com.freestyleperu.aplicacion.facturacion.service.QynexCpeWebhookService;
import com.freestyleperu.aplicacion.pago.exception.WebhookFirmaException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notificaciones de Qynex CPE sobre el resultado de un comprobante ante SUNAT.
 *
 * <p>La empresa se resuelve por el subdominio de la peticion, igual que en el resto de la
 * aplicacion: cada empresa registra en el panel de CPE la URL de SU tienda, asi que la
 * notificacion llega ya acotada a su tenant sin necesidad de llevar el identificador en la
 * ruta, que seria una via para tocar los datos de otra empresa.
 *
 * <p>El cuerpo se lee crudo y no como objeto: la firma se calcula sobre los bytes exactos que
 * se enviaron, y dejar que Spring lo deserialice y volverlo a serializar cambiaria el orden
 * de las claves o el espaciado, con lo que ninguna firma coincidiria.
 */
@RestController
public class QynexCpeWebhookController {

    private final QynexCpeWebhookService service;

    public QynexCpeWebhookController(QynexCpeWebhookService service) {
        this.service = service;
    }

    @PostMapping("/api/webhooks/qynex-cpe")
    public ResponseEntity<Void> recibir(HttpServletRequest request) {
        service.procesar(
                leerCuerpo(request),
                request.getHeader("X-Qynex-Signature"),
                request.getHeader("X-Qynex-Timestamp"));
        // 204 tambien cuando el comprobante no es de esta tienda: para CPE la entrega se
        // hizo, y devolver un error solo la haria reintentar algo que nunca va a cambiar.
        return ResponseEntity.noContent().build();
    }

    private String leerCuerpo(HttpServletRequest request) {
        try {
            return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new WebhookFirmaException("No se pudo leer la notificacion de Qynex CPE");
        }
    }
}
