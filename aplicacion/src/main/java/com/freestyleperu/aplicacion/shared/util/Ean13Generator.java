package com.freestyleperu.aplicacion.shared.util;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera códigos EAN-13 válidos para uso interno de la tienda, con prefijo
 * {@code 775} (no registrado ante GS1, ver docs/01-requisitos.md S-06).
 * Cualquier lector estándar acepta el formato porque el dígito verificador
 * se calcula correctamente.
 */
@Component
public class Ean13Generator {

    private static final String INTERNAL_PREFIX = "775";
    private static final String SEQUENCE_NAME = "BARCODE";

    private final SequenceService sequenceService;

    public Ean13Generator(SequenceService sequenceService) {
        this.sequenceService = sequenceService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String generar() {
        String correlativo = sequenceService.next(SEQUENCE_NAME, INTERNAL_PREFIX, 9);
        String base12 = correlativo.replace("-", "");
        return base12 + checkDigit(base12);
    }

    private int checkDigit(String base12) {
        int sum = 0;
        for (int i = 0; i < base12.length(); i++) {
            int digit = base12.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - (sum % 10)) % 10;
    }
}
