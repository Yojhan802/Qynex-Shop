-- Correlativos con nombre para SKU, código interno y código de barras.
-- Ver docs/03-modelo-datos.md §11.

CREATE TABLE sequences (
    name           VARCHAR(40) NOT NULL PRIMARY KEY,
    prefix         VARCHAR(10) NULL,
    current_value  BIGINT      NOT NULL DEFAULT 0,
    padding        INT         NOT NULL DEFAULT 5
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
