-- Envío profesional para pedidos online: ubicación completa (departamento/
-- provincia/distrito, no solo distrito), tarifa de envío configurable,
-- contraentrega exclusiva para el distrito de Huacho (sede física) y
-- comprobante de pago subido por el cliente. Ver docs/03-modelo-datos.md.

ALTER TABLE company_settings
    ADD COLUMN shipping_flat_rate DECIMAL(12,2) NOT NULL DEFAULT 15.00;

ALTER TABLE orders
    ADD COLUMN department VARCHAR(100) NOT NULL AFTER customer_id,
    ADD COLUMN province VARCHAR(100) NOT NULL AFTER department,
    ADD COLUMN shipping_cost DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER total,
    ADD COLUMN payment_proof_url VARCHAR(255) NULL AFTER payment_reference;

-- Contraentrega: pago en efectivo al momento de la entrega, solo disponible
-- para el distrito de Huacho (envío gratis, recojo/entrega local propia del
-- negocio, no un envío por courier). El backend valida el distrito al crear
-- el pedido — este método de pago nunca afecta caja porque los pedidos
-- online no tocan cash_sessions.
INSERT INTO payment_methods (code, name, type, affects_cash, requires_reference, status, sort_order, created_at, updated_at)
VALUES ('CONTRAENTREGA', 'Contraentrega (solo Huacho)', 'CASH', FALSE, FALSE, 'ACTIVE', 6, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
