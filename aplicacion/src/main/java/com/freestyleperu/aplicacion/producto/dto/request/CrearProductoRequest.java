package com.freestyleperu.aplicacion.producto.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CrearProductoRequest(
        @Size(max = 30) String internalCode,
        @Size(max = 40) String sku,
        @NotBlank @Size(max = 150) String name,
        @NotNull Long categoryId,
        Long subcategoryId,
        Long brandId,
        String description,
        @Size(max = 150) String material,
        @Size(max = 100) String fit,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @DecimalMin(value = "0.01") BigDecimal promoPrice,
        /** Catálogo 07 de SUNAT: 10 gravado, 20 exonerado, 30 inafecto, 40 exportación. */
        @Pattern(regexp = "^(10|20|30|40)$", message = "debe ser 10, 20, 30 o 40") String igvAffectationType,
        /** Catálogo 03 (UN/ECE Rec. 20). Una unidad inexistente la rechaza SUNAT (error 2936). */
        @Size(min = 1, max = 3) String unitCode,
        /** Catálogo 25 (UNSPSC), 8 dígitos. Nulo mientras no se clasifique el producto. */
        @Pattern(regexp = "^(\\d{8})?$", message = "debe tener exactamente 8 dígitos") String sunatProductCode) {

    /**
     * Compatibilidad con las llamadas anteriores a los datos tributarios del producto. Deja
     * los tres campos nulos, y el servicio conserva entonces los valores por defecto de la
     * entidad (gravado y unidades), que es lo que se venía declarando.
     */
    public CrearProductoRequest(
            String internalCode, String sku, String name, Long categoryId, Long subcategoryId,
            Long brandId, String description, String material, String fit,
            BigDecimal price, BigDecimal promoPrice) {
        this(internalCode, sku, name, categoryId, subcategoryId, brandId, description, material,
                fit, price, promoPrice, null, null, null);
    }
}
