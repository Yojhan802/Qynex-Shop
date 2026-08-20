-- Sucursales, almacenes y movimientos de inventario (inmutables).
-- Ver docs/03-modelo-datos.md §6.

CREATE TABLE branches (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255) NULL,
    phone       VARCHAR(20)  NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_branches_code UNIQUE (code),
    CONSTRAINT chk_branches_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE warehouses (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id   BIGINT UNSIGNED NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_warehouses_code UNIQUE (code),
    CONSTRAINT fk_warehouses_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT chk_warehouses_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE inventory_movements (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    variant_id      BIGINT UNSIGNED NOT NULL,
    warehouse_id    BIGINT UNSIGNED NOT NULL,
    type            VARCHAR(20) NOT NULL,
    quantity        INT NOT NULL,
    stock_before    INT NOT NULL,
    stock_after     INT NOT NULL,
    reference_type  VARCHAR(20) NULL,
    reference_id    BIGINT UNSIGNED NULL,
    reason          VARCHAR(255) NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    CONSTRAINT fk_movements_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_movements_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_movements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_movements_type CHECK (type IN ('ENTRADA', 'SALIDA', 'VENTA', 'DEVOLUCION', 'AJUSTE', 'MERMA')),
    CONSTRAINT chk_movements_reference_type CHECK (reference_type IS NULL OR reference_type IN ('SALE', 'RETURN', 'ADJUSTMENT')),
    CONSTRAINT chk_movements_quantity CHECK (quantity <> 0),
    CONSTRAINT chk_movements_consistency CHECK (stock_after = stock_before + quantity)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_movements_variant ON inventory_movements (variant_id, created_at);
CREATE INDEX idx_movements_type ON inventory_movements (type);
CREATE INDEX idx_movements_reference ON inventory_movements (reference_type, reference_id);
