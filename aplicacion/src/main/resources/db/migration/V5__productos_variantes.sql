-- Productos y variantes. Producto ≠ variante (docs/03-modelo-datos.md §5).

CREATE TABLE products (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    internal_code  VARCHAR(30)  NOT NULL,
    sku            VARCHAR(40)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    category_id    BIGINT UNSIGNED NOT NULL,
    subcategory_id BIGINT UNSIGNED NULL,
    brand_id       BIGINT UNSIGNED NULL,
    description    TEXT NULL,
    price          DECIMAL(12,2) NOT NULL,
    promo_price    DECIMAL(12,2) NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    image_url      VARCHAR(255) NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT uk_products_internal_code UNIQUE (internal_code),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategories (id),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_promo_price CHECK (promo_price IS NULL OR promo_price >= 0),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_products_status ON products (status);

CREATE TABLE product_variants (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT UNSIGNED NOT NULL,
    color_id    BIGINT UNSIGNED NOT NULL,
    size_id     BIGINT UNSIGNED NOT NULL,
    sku         VARCHAR(60) NOT NULL,
    barcode     VARCHAR(20) NULL,
    stock       INT NOT NULL DEFAULT 0,
    min_stock   INT NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    CONSTRAINT uk_variants_sku UNIQUE (sku),
    CONSTRAINT uk_variants_barcode UNIQUE (barcode),
    CONSTRAINT uk_variants_combination UNIQUE (product_id, color_id, size_id),
    CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_variants_color FOREIGN KEY (color_id) REFERENCES colors (id),
    CONSTRAINT fk_variants_size FOREIGN KEY (size_id) REFERENCES sizes (id),
    CONSTRAINT chk_variants_stock CHECK (stock >= 0),
    CONSTRAINT chk_variants_min_stock CHECK (min_stock >= 0),
    CONSTRAINT chk_variants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
