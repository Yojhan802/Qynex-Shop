-- Las separaciones de un live no siempre corresponden a un cliente registrado
-- (ver docs/04-reglas-negocio.md RN-27) — solo los que compran por la tienda
-- online quedan como Customer. Para los ocasionales basta nombre y,
-- opcionalmente, teléfono (para ubicar más rápido su comprobante en WhatsApp).

ALTER TABLE reservations MODIFY COLUMN customer_id BIGINT UNSIGNED NULL;

ALTER TABLE reservations
    ADD COLUMN guest_name  VARCHAR(150) NULL AFTER customer_id,
    ADD COLUMN guest_phone VARCHAR(20)  NULL AFTER guest_name;

ALTER TABLE reservations
    ADD CONSTRAINT chk_reservations_buyer CHECK (customer_id IS NOT NULL OR guest_name IS NOT NULL);
