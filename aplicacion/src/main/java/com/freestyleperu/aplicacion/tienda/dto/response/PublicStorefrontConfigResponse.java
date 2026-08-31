package com.freestyleperu.aplicacion.tienda.dto.response;

import com.freestyleperu.aplicacion.configuracion.domain.StoreTemplate;

/** Configuracion publica minima para seleccionar el render visual del storefront. */
public record PublicStorefrontConfigResponse(
        StoreTemplate template,
        String primaryColor,
        String accentColor,
        String backgroundColor) {
}
