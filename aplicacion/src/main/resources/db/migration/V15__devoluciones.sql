-- Devoluciones (docs/03-modelo-datos.md §10). Siempre asociadas a una venta
-- existente; "restock" decide explícitamente si la prenda vuelve al stock.

CREATE TABLE returns (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    return_number     VARCHAR(20) NOT NULL,
    sale_id           BIGINT UNSIGNED NOT NULL,
    user_id           BIGINT UNSIGNED NOT NULL,
    total_amount      DECIMAL(12,2) NOT NULL,
    refund_method_id  BIGINT UNSIGNED NOT NULL,
    reason            VARCHAR(255) NOT NULL,
    created_at        DATETIME(6) NOT NULL,
    CONSTRAINT uk_returns_number UNIQUE (return_number),
    CONSTRAINT fk_returns_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_returns_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_returns_refund_method FOREIGN KEY (refund_method_id) REFERENCES payment_methods (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_returns_sale ON returns (sale_id);

CREATE TABLE return_details (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    return_id       BIGINT UNSIGNED NOT NULL,
    sale_detail_id  BIGINT UNSIGNED NOT NULL,
    variant_id      BIGINT UNSIGNED NOT NULL,
    quantity        INT NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    restock         BOOLEAN NOT NULL,
    CONSTRAINT fk_return_details_return FOREIGN KEY (return_id) REFERENCES returns (id),
    CONSTRAINT fk_return_details_sale_detail FOREIGN KEY (sale_detail_id) REFERENCES sale_details (id),
    CONSTRAINT fk_return_details_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT chk_return_details_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_return_details_return ON return_details (return_id);
