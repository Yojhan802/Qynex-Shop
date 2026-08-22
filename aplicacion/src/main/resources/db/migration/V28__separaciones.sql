-- Separaciones (layaway): el cliente paga una seña para apartar una prenda
-- (stock se retira de inmediato, a diferencia de un pedido online) y
-- completa el resto después, en tienda. Si no completa antes de vencer, la
-- prenda vuelve a stock disponible y la seña se pierde (no hay reversión
-- automática de caja porque la seña nunca la tocó — ver ReservaService).
-- Funcionalidad de plan PROFESIONAL (ver docs/03 §15, RN-23).

ALTER TABLE inventory_movements
    DROP CHECK chk_movements_type,
    ADD CONSTRAINT chk_movements_type
        CHECK (type IN ('ENTRADA', 'SALIDA', 'VENTA', 'DEVOLUCION', 'AJUSTE', 'MERMA', 'RESERVA', 'RESERVA_LIBERADA')),
    DROP CHECK chk_movements_reference_type,
    ADD CONSTRAINT chk_movements_reference_type
        CHECK (reference_type IS NULL OR reference_type IN ('SALE', 'RETURN', 'ADJUSTMENT', 'ORDER', 'RESERVATION'));

ALTER TABLE company_settings
    ADD COLUMN reservation_deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 20.00,
    ADD COLUMN reservation_expiration_days INT NOT NULL DEFAULT 3;

CREATE TABLE reservations (
    id                          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reservation_number          VARCHAR(20)  NOT NULL,
    customer_id                 BIGINT UNSIGNED NOT NULL,
    variant_id                  BIGINT UNSIGNED NOT NULL,
    quantity                    INT NOT NULL,
    unit_price                  DECIMAL(12,2) NOT NULL,
    deposit_amount               DECIMAL(12,2) NOT NULL,
    deposit_payment_method_id   BIGINT UNSIGNED NOT NULL,
    deposit_reference           VARCHAR(50) NULL,
    promoter_id                 BIGINT UNSIGNED NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'RESERVADO',
    expires_at                  DATETIME(6) NOT NULL,
    notes                       VARCHAR(255) NULL,
    created_by                  BIGINT UNSIGNED NOT NULL,
    created_at                  DATETIME(6) NOT NULL,
    sale_id                     BIGINT UNSIGNED NULL,
    completed_at                DATETIME(6) NULL,
    completed_by                BIGINT UNSIGNED NULL,
    cancelled_at                DATETIME(6) NULL,
    cancelled_by                BIGINT UNSIGNED NULL,
    cancellation_reason         VARCHAR(255) NULL,
    CONSTRAINT uk_reservations_number UNIQUE (reservation_number),
    CONSTRAINT fk_reservations_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_reservations_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_reservations_deposit_method FOREIGN KEY (deposit_payment_method_id) REFERENCES payment_methods (id),
    CONSTRAINT fk_reservations_promoter FOREIGN KEY (promoter_id) REFERENCES promoters (id),
    CONSTRAINT fk_reservations_created_by FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT fk_reservations_sale FOREIGN KEY (sale_id) REFERENCES sales (id),
    CONSTRAINT fk_reservations_completed_by FOREIGN KEY (completed_by) REFERENCES users (id),
    CONSTRAINT fk_reservations_cancelled_by FOREIGN KEY (cancelled_by) REFERENCES users (id),
    CONSTRAINT chk_reservations_status CHECK (status IN ('RESERVADO', 'COMPLETADO', 'CANCELADO', 'VENCIDO')),
    CONSTRAINT chk_reservations_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_reservations_status ON reservations (status);
CREATE INDEX idx_reservations_expires ON reservations (expires_at);
CREATE INDEX idx_reservations_variant ON reservations (variant_id);

INSERT INTO permissions (code, module, description) VALUES
    ('RESERVAS_CONSULTAR', 'RESERVAS', 'Consultar separaciones'),
    ('RESERVAS_CREAR', 'RESERVAS', 'Crear separaciones'),
    ('RESERVAS_GESTIONAR', 'RESERVAS', 'Completar o cancelar separaciones');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMINISTRADOR', 'SUPERVISOR') AND p.code IN ('RESERVAS_CONSULTAR', 'RESERVAS_CREAR', 'RESERVAS_GESTIONAR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'VENDEDOR' AND p.code IN ('RESERVAS_CONSULTAR', 'RESERVAS_CREAR');
