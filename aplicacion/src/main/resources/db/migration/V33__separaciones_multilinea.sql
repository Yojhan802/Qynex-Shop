-- Una separación deja de ser "una fila = un producto" y pasa a ser
-- cabecera + líneas (mismo patrón que sales/sale_details): el comprador
-- aparta varios productos de una vez con una sola seña para todo el grupo,
-- y opcionalmente puede incluir un combo elegido explícitamente (botón
-- "+ Agregar combo" en el panel) — ver docs/03-modelo-datos.md §17.
CREATE TABLE reservation_details (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reservation_id   BIGINT UNSIGNED NOT NULL,
    variant_id       BIGINT UNSIGNED NOT NULL,
    quantity         INT NOT NULL,
    unit_price       DECIMAL(12,2) NOT NULL,
    discount_amount  DECIMAL(12,2) NOT NULL DEFAULT 0,
    subtotal         DECIMAL(12,2) NOT NULL,
    combo_id         BIGINT UNSIGNED NULL,
    combo_group      INT NULL,
    CONSTRAINT fk_reservation_details_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id),
    CONSTRAINT fk_reservation_details_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_reservation_details_combo FOREIGN KEY (combo_id) REFERENCES combos (id),
    CONSTRAINT chk_reservation_details_quantity CHECK (quantity > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE INDEX idx_reservation_details_reservation ON reservation_details (reservation_id);
CREATE INDEX idx_reservation_details_variant ON reservation_details (variant_id);

-- Migra cada separación existente (una fila = un producto) a su nueva línea.
INSERT INTO reservation_details (reservation_id, variant_id, quantity, unit_price, discount_amount, subtotal, combo_id, combo_group)
SELECT id, variant_id, quantity, unit_price, 0, unit_price * quantity, NULL, NULL
FROM reservations;

ALTER TABLE reservations
    DROP FOREIGN KEY fk_reservations_variant,
    DROP INDEX idx_reservations_variant,
    DROP COLUMN variant_id,
    DROP COLUMN quantity,
    DROP COLUMN unit_price;
