package com.freestyleperu.aplicacion.shared.util;

import com.freestyleperu.aplicacion.shared.exception.ArchivoInvalidoException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Guarda imágenes subidas (logo de empresa, QR de métodos de pago, etc.) bajo app.uploads.dir. */
@Service
public class ImageUploadService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");

    private final Path uploadsDir;

    public ImageUploadService(@Value("${app.uploads.dir}") String uploadsDir) {
        this.uploadsDir = Path.of(uploadsDir);
    }

    /** Valida, guarda el archivo en {uploadsDir}/{subcarpeta}/ y devuelve la URL pública relativa. */
    public String guardar(MultipartFile file, String subcarpeta) {
        if (file == null || file.isEmpty()) {
            throw new ArchivoInvalidoException("Debes seleccionar un archivo de imagen");
        }
        if (!TIPOS_PERMITIDOS.contains(file.getContentType())) {
            throw new ArchivoInvalidoException("La imagen debe ser PNG, JPG, WEBP o SVG");
        }

        Path destino = uploadsDir.resolve(subcarpeta);
        String nombreArchivo = UUID.randomUUID() + extensionDe(file.getOriginalFilename());
        try {
            Files.createDirectories(destino);
            Files.copy(file.getInputStream(), destino.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo guardar la imagen", ex);
        }

        return "/uploads/" + subcarpeta + "/" + nombreArchivo;
    }

    private String extensionDe(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) return "";
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }
}
