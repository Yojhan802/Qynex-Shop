package com.freestyleperu.aplicacion.shared.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class TextNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-zA-Z0-9]+");

    private TextNormalizer() {
    }

    public static String slugify(String text) {
        String withoutAccents = stripAccents(text);
        String slug = NON_ALPHANUMERIC.matcher(withoutAccents).replaceAll("-").toLowerCase();
        return slug.replaceAll("^-+|-+$", "");
    }

    /** Prefijo de 3 letras en mayúscula para generar SKU (p. ej. "Polos" -> "POL"). */
    public static String prefix3(String text) {
        String letters = NON_ALPHANUMERIC.matcher(stripAccents(text)).replaceAll("").toUpperCase();
        return letters.length() >= 3 ? letters.substring(0, 3) : String.format("%-3s", letters).replace(' ', 'X');
    }

    private static String stripAccents(String text) {
        return DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
    }
}
