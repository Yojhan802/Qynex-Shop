package com.freestyleperu.aplicacion.reclamo.service;

import com.freestyleperu.aplicacion.reclamo.domain.ComplaintBookEntry;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * Constancia de la hoja de reclamación en PDF (D.S. 011-2011-PCM, Art. 5).
 *
 * <p>Se genera al vuelo cada vez y no se guarda en disco: los campos que la componen son
 * inmutables una vez registrada la hoja, así que regenerarla da siempre el mismo documento,
 * y no almacenarla evita repetir la exposición que ya hubo con los archivos de /uploads.
 *
 * <p>Se usa OpenPDF y no una librería de bajo nivel porque el detalle admite 5000 caracteres:
 * hace falta salto de línea y de página automáticos. Perder texto por desbordar la página
 * sería entregar una copia incompleta de lo que el consumidor declaró.
 */
@Service
public class ConstanciaPdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15);
    private static final Font SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font CUERPO = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font PIE = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f);

    public byte[] generar(ComplaintBookEntry entry, int plazoRespuestaDias) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 48, 48, 48, 48);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(parrafo("HOJA DE RECLAMACIÓN", TITULO, 4));
            documento.add(parrafo("N° " + entry.getEntryNumber() + "  ·  "
                    + entry.getCreatedAt().format(FECHA), CUERPO, 14));

            seccion(documento, "PROVEEDOR");
            dato(documento, "Razón social", entry.getProviderName());
            dato(documento, "RUC", entry.getProviderRuc());
            dato(documento, "Domicilio", entry.getProviderAddress());

            seccion(documento, "CONSUMIDOR");
            dato(documento, "Nombre", entry.getConsumerName());
            dato(documento, "Documento", entry.getConsumerDocument());
            dato(documento, "Domicilio", entry.getConsumerAddress());
            dato(documento, "Correo", entry.getConsumerEmail());
            dato(documento, "Teléfono", entry.getConsumerPhone());

            seccion(documento, "IDENTIFICACIÓN DEL BIEN CONTRATADO");
            dato(documento, "Tipo", entry.getType().name());
            dato(documento, "Descripción", entry.getProductServiceDescription());
            dato(documento, "Pedido", entry.getOrderNumber());
            dato(documento, "Monto reclamado", entry.getAmount() == null ? null : entry.getAmount().toPlainString());

            seccion(documento, "DETALLE");
            documento.add(parrafo(entry.getDetail(), CUERPO, 10));

            seccion(documento, "PEDIDO DEL CONSUMIDOR");
            documento.add(parrafo(entry.getConsumerRequest(), CUERPO, 16));

            LocalDate vence = entry.getCreatedAt().toLocalDate().plusDays(plazoRespuestaDias);
            documento.add(parrafo("El proveedor debe dar respuesta en un plazo máximo de "
                    + plazoRespuestaDias + " días calendario, es decir hasta el "
                    + vence.format(DIA) + " (D.S. 011-2011-PCM).", CUERPO, 8));
            documento.add(parrafo("La formulación del reclamo no impide acudir a otras vías de "
                    + "solución de controversias ni es requisito previo para denunciar ante INDECOPI.",
                    PIE, 0));

            documento.close();
            return salida.toByteArray();
        } catch (DocumentException ex) {
            documento.close();
            throw new IllegalStateException("No se pudo generar la constancia en PDF", ex);
        } catch (UncheckedIOException ex) {
            documento.close();
            throw ex;
        }
    }

    private void seccion(Document documento, String titulo) throws DocumentException {
        documento.add(parrafo(titulo, SECCION, 3));
    }

    /** Los campos opcionales que llegan vacíos se omiten en vez de imprimir una línea huérfana. */
    private void dato(Document documento, String etiqueta, String valor) throws DocumentException {
        if (valor == null || valor.isBlank()) return;
        documento.add(parrafo(etiqueta + ": " + valor, CUERPO, 2));
    }

    private Paragraph parrafo(String texto, Font fuente, float espacioDespues) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_LEFT);
        parrafo.setSpacingAfter(espacioDespues);
        return parrafo;
    }
}
