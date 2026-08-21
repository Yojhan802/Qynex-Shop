package com.freestyleperu.aplicacion.busqueda.dto.response;

/** `url` es una ruta relativa del frontend admin a la que navegar al elegir el resultado. */
public record SearchResultItem(String type, Long id, String title, String subtitle, String url) {
}
