-- Métodos de pago (docs/03-modelo-datos.md §8). Solo el efectivo afecta el
-- arqueo de caja (affects_cash); los datos oficiales de Yape/Plin solo los
-- edita quien tenga CONFIGURACION_PAGOS (RN-15).

CREATE TABLE payment_methods (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL,
    name                VARCHAR(40)  NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    affects_cash        BOOLEAN      NOT NULL,
    requires_reference  BOOLEAN      NOT NULL,
    account_holder      VARCHAR(150) NULL,
    account_number      VARCHAR(30)  NULL,
    qr_image_url        VARCHAR(255) NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    sort_order          SMALLINT     NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,
    CONSTRAINT uk_payment_methods_code UNIQUE (code),
    CONSTRAINT chk_payment_methods_type CHECK (type IN ('CASH', 'DIGITAL_WALLET', 'CARD', 'TRANSFER')),
    CONSTRAINT chk_payment_methods_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO payment_methods (code, name, type, affects_cash, requires_reference, status, sort_order, created_at, updated_at) VALUES
    ('EFECTIVO',      'Efectivo',      'CASH',           TRUE,  FALSE, 'ACTIVE', 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('YAPE',          'Yape',          'DIGITAL_WALLET', FALSE, TRUE,  'ACTIVE', 2, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('PLIN',          'Plin',          'DIGITAL_WALLET', FALSE, TRUE,  'ACTIVE', 3, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('TARJETA',       'Tarjeta',       'CARD',           FALSE, TRUE,  'ACTIVE', 4, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('TRANSFERENCIA', 'Transferencia', 'TRANSFER',       FALSE, TRUE,  'ACTIVE', 5, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
