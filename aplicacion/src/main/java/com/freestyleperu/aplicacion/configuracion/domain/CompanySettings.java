package com.freestyleperu.aplicacion.configuracion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Fila única (id = 1). Ver docs/03-modelo-datos.md "company_settings". */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_settings")
public class CompanySettings {

    @Id
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "ruc", length = 15)
    private String ruc;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "currency_symbol", nullable = false, length = 5)
    private String currencySymbol;

    @Column(name = "igv_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal igvRate;

    @Column(name = "ticket_footer", length = 255)
    private String ticketFooter;

    @Column(name = "shipping_flat_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingFlatRate;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private Long updatedBy;
}
