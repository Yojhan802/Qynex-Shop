package com.freestyleperu.aplicacion.producto.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActualizarProductoRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull Long categoryId,
        Long subcategoryId,
        Long brandId,
        String description,
        @Size(max = 150) String material,
        @Size(max = 100) String fit,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @DecimalMin(value = "0.01") BigDecimal promoPrice,
        String imageUrl,
        /** Catálogo 07 de SUNAT: 10 gravado, 20 exonerado, 30 inafecto, 40 exportación. */
        @Pattern(regexp = "^(10|20|30|40)$", message = "debe ser 10, 20, 30 o 40") String igvAffectationType,
        /** Catálogo 03 (UN/ECE Rec. 20). Una unidad inexistente la rechaza SUNAT (error 2936). */
        @Size(min = 1, max = 3) String unitCode,
        /** Catálogo 25 (UNSPSC), 8 dígitos. Nulo mientras no se clasifique el producto. */
        @Pattern(regexp = "^(\\d{8})?$", message = "debe tener exactamente 8 dígitos") String sunatProductCode) {

    /**
     * Compatibilidad con las llamadas anteriores a los datos tributarios del producto. Con
     * los tres nulos el servicio conserva la clasificación que ya tuviera el producto, en
     * vez de degradarla por una petición que no conocía estos campos.
     */
    public ActualizarProductoRequest(
            String name, Long categoryId, Long subcategoryId, Long brandId, String description,
            String material, String fit, BigDecimal price, BigDecimal promoPrice, String imageUrl) {
        this(name, categoryId, subcategoryId, brandId, description, material, fit, price,
                promoPrice, imageUrl, null, null, null);
    }
}
