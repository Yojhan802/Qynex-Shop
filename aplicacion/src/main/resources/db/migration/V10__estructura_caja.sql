-- Cajas, sesiones y movimientos (docs/03-modelo-datos.md §9).

CREATE TABLE cash_registers (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    branch_id   BIGINT UNSIGNED NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT uk_cash_registers_code UNIQUE (code),
    CONSTRAINT fk_cash_registers_branch FOREIGN KEY (branch_id) REFERENCES branches (id),
    CONSTRAINT chk_cash_registers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE cash_sessions (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cash_register_id  BIGINT UNSIGNED NOT NULL,
    opened_by         BIGINT UNSIGNED NOT NULL,
    opening_amount    DECIMAL(12,2) NOT NULL,
    opened_at         DATETIME(6) NOT NULL,
    expected_amount   DECIMAL(12,2) NULL,
    counted_amount    DECIMAL(12,2) NULL,
    difference        DECIMAL(12,2) NULL,
    closed_by         BIGINT UNSIGNED NULL,
    closed_at         DATETIME(6) NULL,
    status            VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    notes             VARCHAR(255) NULL,
    -- Garantiza "una sola sesión abierta por caja" (RN-09) incluso ante
    -- condiciones de carrera: solo hay un valor no-nulo posible por caja.
    open_register_id  BIGINT UNSIGNED GENERATED ALWAYS AS (CASE WHEN status = 'OPEN' THEN cash_register_id END) VIRTUAL,
    CONSTRAINT fk_cash_sessions_register FOREIGN KEY (cash_register_id) REFERENCES cash_registers (id),
    CONSTRAINT fk_cash_sessions_opened_by FOREIGN KEY (opened_by) REFERENCES users (id),
    CONSTRAINT fk_cash_sessions_closed_by FOREIGN KEY (closed_by) REFERENCES users (id),
    CONSTRAINT chk_cash_sessions_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT uk_one_open_session UNIQUE (open_register_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_cash_sessions_opened_by ON cash_sessions (opened_by, status);

CREATE TABLE cash_movements (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cash_session_id  BIGINT UNSIGNED NOT NULL,
    type             VARCHAR(15) NOT NULL,
    amount           DECIMAL(12,2) NOT NULL,
    reference_type   VARCHAR(10) NULL,
    reference_id     BIGINT UNSIGNED NULL,
    reason           VARCHAR(255) NULL,
    user_id          BIGINT UNSIGNED NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    CONSTRAINT fk_cash_movements_session FOREIGN KEY (cash_session_id) REFERENCES cash_sessions (id),
    CONSTRAINT fk_cash_movements_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_cash_movements_type CHECK (type IN ('VENTA', 'INGRESO', 'GASTO', 'RETIRO', 'DEVOLUCION')),
    CONSTRAINT chk_cash_movements_reference_type CHECK (reference_type IS NULL OR reference_type IN ('SALE', 'RETURN')),
    CONSTRAINT chk_cash_movements_amount CHECK (amount <> 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_cash_movements_session ON cash_movements (cash_session_id, created_at);
