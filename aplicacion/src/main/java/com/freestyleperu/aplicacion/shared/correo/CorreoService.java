package com.freestyleperu.aplicacion.shared.correo;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envío de correo saliente.
 *
 * <p>Está pensado para no ser nunca la causa de que falle una operación de negocio: si no
 * hay SMTP configurado, o si el envío falla, {@link #enviar} devuelve {@code false} y deja
 * rastro en el log en vez de propagar la excepción. Quien llama decide qué hacer con eso,
 * pero lo que ya se registró en la base queda registrado.
 *
 * <p>El caso que lo motiva es la constancia de la hoja de reclamación: el D.S. 011-2011-PCM
 * obliga a entregarla al consumidor, pero un SMTP caído no puede impedir que el consumidor
 * deje su reclamo — eso lo dejaría sin ambas cosas.
 */
@Service
public class CorreoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoService.class);

    private final JavaMailSender mailSender;
    private final String host;
    private final String remitente;
    private final String remitenteNombre;

    public CorreoService(JavaMailSender mailSender,
            @Value("${spring.mail.host:}") String host,
            @Value("${app.correo.remitente:}") String remitente,
            @Value("${app.correo.remitente-nombre:Qynex Shop}") String remitenteNombre) {
        this.mailSender = mailSender;
        this.host = host == null ? "" : host.trim();
        this.remitente = remitente == null ? "" : remitente.trim();
        this.remitenteNombre = remitenteNombre;
    }

    /** Hay SMTP y remitente. Sin las dos cosas no se intenta enviar nada. */
    public boolean configurado() {
        return !host.isBlank() && !remitente.isBlank();
    }

    /**
     * Envía un correo de texto plano. Devuelve si salió, nunca lanza.
     *
     * <p>Texto plano y no HTML a propósito: son documentos que el destinatario debe poder
     * archivar y releer en cualquier cliente de correo, y el contenido es lo que tiene
     * valor legal, no el formato.
     */
    public boolean enviar(String destino, String asunto, String cuerpo) {
        return enviar(destino, asunto, cuerpo, null, null);
    }

    /**
     * Igual, con un adjunto opcional. Si el adjunto viene nulo se manda como correo simple.
     */
    public boolean enviar(String destino, String asunto, String cuerpo, String nombreAdjunto, byte[] adjunto) {
        if (!configurado()) {
            log.warn("Correo no configurado (falta MAIL_HOST o MAIL_FROM): no se envía «{}» a {}", asunto, destino);
            return false;
        }
        if (destino == null || destino.isBlank()) {
            log.warn("Sin destinatario para «{}»", asunto);
            return false;
        }
        try {
            boolean conAdjunto = adjunto != null && adjunto.length > 0 && nombreAdjunto != null;
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, conAdjunto, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(remitente, remitenteNombre, StandardCharsets.UTF_8.name()));
            helper.setTo(destino.trim());
            helper.setSubject(asunto);
            helper.setText(cuerpo, false);
            if (conAdjunto) {
                helper.addAttachment(nombreAdjunto, new ByteArrayResource(adjunto));
            }
            mailSender.send(mensaje);
            log.info("Correo enviado: «{}» a {}", asunto, destino);
            return true;
        } catch (UnsupportedEncodingException | jakarta.mail.MessagingException | RuntimeException ex) {
            // Se traga la excepción a propósito: ver el Javadoc de la clase.
            log.error("No se pudo enviar «{}» a {}: {}", asunto, destino, ex.getMessage());
            return false;
        }
    }
}
