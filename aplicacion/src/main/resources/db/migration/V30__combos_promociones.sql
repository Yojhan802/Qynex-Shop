-- Combos: set fijo de productos a un precio total fijo (docs/04-reglas-negocio.md RN-28).
-- El combo define productos, no variantes exactas — el cajero elige color/talla
-- de cada producto del combo al venderlo (ver docs/03-modelo-datos.md §18).
CREATE TABLE combos (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(30)   NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    price       DECIMAL(12,2) NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT uk_combos_code UNIQUE (code),
    CONSTRAINT chk_combos_price CHECK (price > 0)
);

CREATE TABLE combo_items (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    combo_id    BIGINT UNSIGNED NOT NULL,
    product_id  BIGINT UNSIGNED NOT NULL,
    quantity    INT NOT NULL,
    CONSTRAINT fk_combo_items_combo FOREIGN KEY (combo_id) REFERENCES combos (id),
    CONSTRAINT fk_combo_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_combo_items_quantity CHECK (quantity > 0)
);

-- Promociones: % o monto fijo, con alcance opcional (todo / una categoría /
-- un producto) y vigencia opcional por fechas. Nunca se aplican solas — el
-- cajero las elige por línea de venta (RN-28: la exclusividad de canal, ej.
-- "solo para el live", es criterio del vendedor, no algo que el sistema fuerce).
CREATE TABLE promotions (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    code                VARCHAR(30)   NOT NULL,
    name                VARCHAR(150)  NOT NULL,
    discount_type       VARCHAR(20)   NOT NULL,
    discount_value      DECIMAL(12,2) NOT NULL,
    scope_type          VARCHAR(20)   NOT NULL DEFAULT 'ALL',
    scope_category_id   BIGINT UNSIGNED NULL,
    scope_product_id    BIGINT UNSIGNED NULL,
    starts_at           DATETIME(6) NULL,
    ends_at             DATETIME(6) NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at          DATETIME(6) NOT NULL,
    updated_at          DATETIME(6) NOT NULL,
    CONSTRAINT uk_promotions_code UNIQUE (code),
    CONSTRAINT fk_promotions_category FOREIGN KEY (scope_category_id) REFERENCES categories (id),
    CONSTRAINT fk_promotions_product FOREIGN KEY (scope_product_id) REFERENCES products (id),
    CONSTRAINT chk_promotions_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT')),
    CONSTRAINT chk_promotions_scope CHECK (scope_type IN ('ALL', 'CATEGORY', 'PRODUCT')),
    CONSTRAINT chk_promotions_value CHECK (discount_value > 0),
    CONSTRAINT chk_promotions_percentage CHECK (discount_type <> 'PERCENTAGE' OR discount_value <= 100)
);

-- Trazabilidad: qué línea de una venta vino de un combo y/o tuvo una
-- promoción aplicada — para poder reportar "combos vendidos" / "ventas por
-- promoción" sin una estructura de reportes paralela (mismo criterio D-05).
ALTER TABLE sale_details
    ADD COLUMN combo_id     BIGINT UNSIGNED NULL AFTER variant_id,
    ADD COLUMN promotion_id BIGINT UNSIGNED NULL AFTER discount_amount;

ALTER TABLE sale_details
    ADD CONSTRAINT fk_sale_details_combo FOREIGN KEY (combo_id) REFERENCES combos (id),
    ADD CONSTRAINT fk_sale_details_promotion FOREIGN KEY (promotion_id) REFERENCES promotions (id);

INSERT INTO permissions (code, module, description) VALUES
    ('COMBOS_CONSULTAR', 'COMBOS', 'Ver combos disponibles'),
    ('COMBOS_GESTIONAR', 'COMBOS', 'Crear, editar y desactivar combos'),
    ('PROMOCIONES_CONSULTAR', 'PROMOCIONES', 'Ver promociones disponibles'),
    ('PROMOCIONES_GESTIONAR', 'PROMOCIONES', 'Crear, editar y desactivar promociones'),
    ('PROMOCIONES_APLICAR', 'PROMOCIONES', 'Aplicar una promoción a una línea de venta');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMINISTRADOR', 'SUPERVISOR')
  AND p.code IN ('COMBOS_CONSULTAR', 'COMBOS_GESTIONAR', 'PROMOCIONES_CONSULTAR', 'PROMOCIONES_GESTIONAR', 'PROMOCIONES_APLICAR');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'VENDEDOR'
  AND p.code IN ('COMBOS_CONSULTAR', 'PROMOCIONES_CONSULTAR', 'PROMOCIONES_APLICAR');
