-- Clientes (docs/03-modelo-datos.md §7).

CREATE TABLE customers (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    doc_type    VARCHAR(20)  NOT NULL DEFAULT 'SIN_DOCUMENTO',
    doc_number  VARCHAR(15)  NULL,
    phone       VARCHAR(20)  NULL,
    email       VARCHAR(120) NULL,
    birth_date  DATE NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT uk_customers_doc_number UNIQUE (doc_number),
    CONSTRAINT chk_customers_doc_type CHECK (doc_type IN ('DNI', 'RUC', 'CE', 'SIN_DOCUMENTO')),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_customers_full_name ON customers (full_name);
