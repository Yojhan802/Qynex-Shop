-- Datos de la empresa mostrados en el ticket y usados por el cálculo de IGV
-- (docs/03-modelo-datos.md §"company_settings"). Fila única, id fijo = 1.

CREATE TABLE company_settings (
    id               BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    name             VARCHAR(150)  NOT NULL,
    ruc              VARCHAR(15)   NULL,
    address          VARCHAR(255)  NULL,
    phone            VARCHAR(20)   NULL,
    email            VARCHAR(120)  NULL,
    logo_url         VARCHAR(255)  NULL,
    currency_code    VARCHAR(3)    NOT NULL DEFAULT 'PEN',
    currency_symbol  VARCHAR(5)    NOT NULL DEFAULT 'S/',
    igv_rate         DECIMAL(5,4)  NOT NULL DEFAULT 0.1800,
    ticket_footer    VARCHAR(255)  NULL,
    updated_at       DATETIME(6)   NOT NULL,
    updated_by       BIGINT UNSIGNED NULL,
    CONSTRAINT chk_company_settings_singleton CHECK (id = 1),
    CONSTRAINT fk_company_settings_updated_by FOREIGN KEY (updated_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

INSERT INTO company_settings (id, name, ruc, address, phone, email, currency_code, currency_symbol, igv_rate, ticket_footer, updated_at, updated_by) VALUES
    (1, 'Freestyle Perú', NULL, NULL, NULL, NULL, 'PEN', 'S/', 0.1800, 'Gracias por su compra', UTC_TIMESTAMP(6), NULL);
