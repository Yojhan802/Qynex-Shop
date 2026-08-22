-- Una línea de combo no siempre es un producto específico — ofertas reales
-- como "4 polos de esta marca por S/100" necesitan "N unidades de cualquier
-- producto de esta categoría (y opcionalmente esta marca)" (RN-28, ver
-- docs/03-modelo-datos.md §18). selector_type distingue ambos casos.

ALTER TABLE combo_items MODIFY COLUMN product_id BIGINT UNSIGNED NULL;

ALTER TABLE combo_items
    ADD COLUMN selector_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT' AFTER combo_id,
    ADD COLUMN category_id   BIGINT UNSIGNED NULL AFTER product_id,
    ADD COLUMN brand_id      BIGINT UNSIGNED NULL AFTER category_id;

ALTER TABLE combo_items
    ADD CONSTRAINT fk_combo_items_category FOREIGN KEY (category_id) REFERENCES categories (id),
    ADD CONSTRAINT fk_combo_items_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    ADD CONSTRAINT chk_combo_items_selector_type CHECK (selector_type IN ('PRODUCT', 'CATEGORY')),
    ADD CONSTRAINT chk_combo_items_selector CHECK (
        (selector_type = 'PRODUCT' AND product_id IS NOT NULL AND category_id IS NULL)
        OR
        (selector_type = 'CATEGORY' AND category_id IS NOT NULL AND product_id IS NULL)
    );
