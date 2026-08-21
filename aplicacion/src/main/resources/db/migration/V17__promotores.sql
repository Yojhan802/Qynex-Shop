-- Promotores: personal de piso que ofrece el producto pero no necesariamente
-- opera la caja. No son usuarios del sistema (no requieren login). Se
-- registran opcionalmente en la venta solo para medir comisión/desempeño;
-- nunca aparecen en el ticket, que siempre muestra al usuario que hizo la venta.

CREATE TABLE promoters (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT chk_promoters_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE sales
    ADD COLUMN promoter_id BIGINT UNSIGNED NULL AFTER user_id,
    ADD CONSTRAINT fk_sales_promoter FOREIGN KEY (promoter_id) REFERENCES promoters (id);

CREATE INDEX idx_sales_promoter ON sales (promoter_id);

INSERT INTO permissions (code, module, description) VALUES
    ('PROMOTORES_CONSULTAR', 'PROMOTORES', 'Consultar promotores'),
    ('PROMOTORES_GESTIONAR', 'PROMOTORES', 'Crear y editar promotores');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMINISTRADOR', 'SUPERVISOR') AND p.code IN ('PROMOTORES_CONSULTAR', 'PROMOTORES_GESTIONAR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'VENDEDOR' AND p.code = 'PROMOTORES_CONSULTAR';
