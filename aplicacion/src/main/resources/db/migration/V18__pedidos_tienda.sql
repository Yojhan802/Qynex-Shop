-- Fase 2 (MVP): cuentas de cliente para la tienda pública, pedidos online y
-- su propio flujo de estados (independiente de `sales`, que exige cajero y
-- caja abierta). Ver docs/03-modelo-datos.md.

ALTER TABLE customers
    ADD COLUMN password_hash VARCHAR(100) NULL AFTER email,
    ADD CONSTRAINT uk_customers_email UNIQUE (email);

-- Refresh tokens de clientes, separados de `refresh_tokens` (que es de staff).
CREATE TABLE customer_refresh_tokens (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT UNSIGNED NOT NULL,
    token_hash   VARCHAR(64)     NOT NULL,
    expires_at   DATETIME(6)     NOT NULL,
    revoked_at   DATETIME(6)     NULL,
    created_at   DATETIME(6)     NOT NULL,
    CONSTRAINT uk_customer_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_customer_refresh_tokens_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_customer_refresh_tokens_customer ON customer_refresh_tokens (customer_id);

CREATE TABLE orders (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_number          VARCHAR(20) NOT NULL,
    customer_id           BIGINT UNSIGNED NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    subtotal              DECIMAL(12,2) NOT NULL,
    total                 DECIMAL(12,2) NOT NULL,
    payment_method_id     BIGINT UNSIGNED NOT NULL,
    payment_reference     VARCHAR(50) NULL,
    recipient_name        VARCHAR(150) NOT NULL,
    phone                 VARCHAR(20) NOT NULL,
    address               VARCHAR(255) NOT NULL,
    district              VARCHAR(100) NOT NULL,
    notes                 VARCHAR(255) NULL,
    created_at            DATETIME(6) NOT NULL,
    confirmed_at          DATETIME(6) NULL,
    confirmed_by          BIGINT UNSIGNED NULL,
    cancelled_at          DATETIME(6) NULL,
    cancellation_reason   VARCHAR(255) NULL,
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_orders_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_methods (id),
    CONSTRAINT fk_orders_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id),
    CONSTRAINT chk_orders_total CHECK (total >= 0),
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_orders_created ON orders (created_at);
CREATE INDEX idx_orders_customer ON orders (customer_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_details (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT UNSIGNED NOT NULL,
    variant_id    BIGINT UNSIGNED NOT NULL,
    quantity      INT NOT NULL,
    unit_price    DECIMAL(12,2) NOT NULL,
    subtotal      DECIMAL(12,2) NOT NULL,
    product_name  VARCHAR(150) NOT NULL,
    variant_sku   VARCHAR(60) NOT NULL,
    color_name    VARCHAR(40) NOT NULL,
    size_name     VARCHAR(20) NOT NULL,
    CONSTRAINT fk_order_details_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_details_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT chk_order_details_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_order_details_order ON order_details (order_id);

-- El stock recién se descuenta cuando el staff confirma el pago (ver plan),
-- momento en el que ese movimiento necesita poder referenciar un pedido.
ALTER TABLE inventory_movements
    DROP CHECK chk_movements_reference_type,
    ADD CONSTRAINT chk_movements_reference_type
        CHECK (reference_type IS NULL OR reference_type IN ('SALE', 'RETURN', 'ADJUSTMENT', 'ORDER'));

INSERT INTO permissions (code, module, description) VALUES
    ('PEDIDOS_CONSULTAR', 'PEDIDOS', 'Consultar pedidos de la tienda online'),
    ('PEDIDOS_GESTIONAR', 'PEDIDOS', 'Confirmar pago o cancelar pedidos de la tienda online');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMINISTRADOR', 'SUPERVISOR') AND p.code IN ('PEDIDOS_CONSULTAR', 'PEDIDOS_GESTIONAR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'VENDEDOR' AND p.code = 'PEDIDOS_CONSULTAR';
