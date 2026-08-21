package com.freestyleperu.aplicacion.busqueda.dto.response;

import java.util.List;

public record SearchResponse(
        List<SearchResultItem> products,
        List<SearchResultItem> customers,
        List<SearchResultItem> sales,
        List<SearchResultItem> users) {
}
