package com.freestyleperu.aplicacion.shared.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Devuelve un archivo privado por HTTP: los que no pueden salir por la ruta estática de
 * {@code /uploads} porque llevan datos de una persona (comprobantes de pago, sobre todo) y
 * necesitan que un endpoint compruebe antes quién pregunta.
 */
public final class ArchivoHttp {

    private ArchivoHttp() {
    }

    /** Para mostrar en el navegador, no descargar: es lo que espera un {@code <img>}. */
    public static ResponseEntity<byte[]> enLinea(ImageUploadService.ArchivoServido archivo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(archivo.contentType()));
        headers.setContentLength(archivo.contenido().length);
        headers.setContentDisposition(ContentDisposition.inline().filename(archivo.nombre()).build());
        // El archivo es de una sola persona: que no quede en cachés compartidas por el camino.
        headers.setCacheControl("private, no-store");
        return new ResponseEntity<>(archivo.contenido(), headers, 200);
    }
}
