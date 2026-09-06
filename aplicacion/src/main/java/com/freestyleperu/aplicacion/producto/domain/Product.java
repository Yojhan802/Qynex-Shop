package com.freestyleperu.aplicacion.producto.domain;

import com.freestyleperu.aplicacion.catalogo.domain.Brand;
import com.freestyleperu.aplicacion.catalogo.domain.Category;
import com.freestyleperu.aplicacion.catalogo.domain.Subcategory;
import com.freestyleperu.aplicacion.shared.domain.BaseEntity;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenant_id", "internal_code" }),
        @UniqueConstraint(columnNames = { "tenant_id", "sku" }) })
public class Product extends BaseEntity {

    @Column(name = "internal_code", nullable = false, length = 30)
    private String internalCode;

    @Column(name = "sku", nullable = false, length = 40)
    private String sku;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private Subcategory subcategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "promo_price", precision = 12, scale = 2)
    private BigDecimal promoPrice;

    /**
     * Catálogo 07 de SUNAT: 10 gravado, 20 exonerado, 30 inafecto, 40 exportación.
     * Pertenece al producto y no a la venta — un libro es exonerado siempre.
     */
    @Column(name = "igv_affectation_type", nullable = false, length = 2)
    private String igvAffectationType = "10";

    /**
     * Catálogo 03 (UN/ECE Rec. 20): NIU unidades, ZZ servicios, KGM kilos. No se valida
     * en local: una unidad inexistente la rechaza SUNAT con el error 2936, y el rechazo
     * quema el correlativo.
     */
    @Column(name = "unit_code", nullable = false, length = 3)
    private String unitCode = "NIU";

    /**
     * Catálogo 25 (UNSPSC), 8 dígitos. Nulo mientras no se clasifique: hoy omitirlo es
     * una observación, pero desde el 01.01.2027 el error 3496 pasa a rechazo.
     */
    @Column(name = "sunat_product_code", length = 8)
    private String sunatProductCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EstadoGeneral status = EstadoGeneral.ACTIVE;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "material", length = 150)
    private String material;

    @Column(name = "fit", length = 100)
    private String fit;

    @Column(name = "size_guide_image_url", length = 255)
    private String sizeGuideImageUrl;
}
