-- Ventas, detalle y pagos (docs/03-modelo-datos.md §8).

CREATE TABLE sales (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sale_number           VARCHAR(20) NOT NULL,
    customer_id           BIGINT UNSIGNED NULL,
    user_id               BIGINT UNSIGNED NOT NULL,
    cash_session_id       BIGINT UNSIGNED NOT NULL,
    subtotal              DECIMAL(12,2) NOT NULL,
    discount_amount       DECIMAL(12,2) NOT NULL DEFAULT 0,
    total                 DECIMAL(12,2) NOT NULL,
    status                VARCHAR(25) NOT NULL DEFAULT 'COMPLETED',
    notes                 VARCHAR(255) NULL,
    created_at            DATETIME(6) NOT NULL,
    cancelled_at          DATETIME(6) NULL,
    cancelled_by          BIGINT UNSIGNED NULL,
    cancellation_reason   VARCHAR(255) NULL,
    CONSTRAINT uk_sales_number UNIQUE (sale_number),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sales_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_sales_cash_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id),
    CONSTRAINT fk_sales_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT chk_sales_total CHECK (total >= 0),
    CONSTRAINT chk_sales_status CHECK (status IN ('COMPLETED', 'CANCELLED', 'RETURNED', 'PARTIALLY_RETURNED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_sales_created ON sales (created_at);
CREATE INDEX idx_sales_user ON sales (user_id);
CREATE INDEX idx_sales_customer ON sales (customer_id);
CREATE INDEX idx_sales_status ON sales (status);

CREATE TABLE sale_details (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sale_id          BIGINT UNSIGNED NOT NULL,
    variant_id       BIGINT UNSIGNED NOT NULL,
    quantity         INT NOT NULL,
    unit_price       DECIMAL(12,2) NOT NULL,
    discount_amount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    subtotal         DECIMAL(12,2) NOT NULL,
    product_name     VARCHAR(150) NOT NULL,
    variant_sku      VARCHAR(60) NOT NULL,
    color_name       VARCHAR(40) NOT NULL,
    size_name        VARCHAR(20) NOT NULL,
    CONSTRAINT fk_sale_details_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_sale_details_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT chk_sale_details_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_sale_details_sale ON sale_details (sale_id);

CREATE TABLE payments (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    sale_id            BIGINT UNSIGNED NOT NULL,
    payment_method_id  BIGINT UNSIGNED NOT NULL,
    amount             DECIMAL(12,2) NOT NULL,
    reference          VARCHAR(50) NULL,
    status             VARCHAR(10) NOT NULL DEFAULT 'COMPLETED',
    created_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_payments_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_payments_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'COMPLETED', 'REFUNDED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_payments_sale ON payments (sale_id);
