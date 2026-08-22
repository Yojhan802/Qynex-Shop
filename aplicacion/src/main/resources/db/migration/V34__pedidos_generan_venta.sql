-- Un pedido online confirmado genera una Sale real (aparece en Ventas, tiene
-- ticket) en vez de quedar como un flujo paralelo — ver PedidoService.
-- Los pedidos online nunca pasan por una sesión de caja física, así que
-- cash_session_id deja de ser obligatorio en sales; shipping_amount permite
-- representar el envío del pedido dentro del total de la venta.
ALTER TABLE sales
    MODIFY COLUMN cash_session_id BIGINT UNSIGNED NULL,
    ADD COLUMN shipping_amount DECIMAL(12,2) NOT NULL DEFAULT 0 AFTER discount_amount;

ALTER TABLE orders
    ADD COLUMN sale_id BIGINT UNSIGNED NULL AFTER cancellation_reason,
    ADD CONSTRAINT fk_orders_sale FOREIGN KEY (sale_id) REFERENCES sales (id);
