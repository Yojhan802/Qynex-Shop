package com.freestyleperu.aplicacion.pedido.domain;

import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Mismo patrón de snapshot que SaleDetail (decisión D-05): el histórico del pedido no cambia si el producto cambia después. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_details")
public class PedidoDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "variant_sku", nullable = false, length = 60)
    private String variantSku;

    @Column(name = "color_name", nullable = false, length = 40)
    private String colorName;

    @Column(name = "size_name", nullable = false, length = 20)
    private String sizeName;
}
