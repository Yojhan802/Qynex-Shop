-- El plazo de cambio voluntario deja de ser una constante del frontend.
--
-- Estaba fijado en 7 días para todas las empresas, pero es una decisión comercial
-- de cada negocio, no de la plataforma: cada cliente lo publica en SUS términos.
-- El default 7 conserva exactamente lo que hoy dice el texto publicado, así que
-- ninguna empresa cambia de política al migrar.
--
-- 0 es válido y significa que el negocio no ofrece cambio voluntario. La garantía
-- legal por falta de idoneidad (Ley 29571) es independiente de este número y no se
-- puede reducir, así que 0 no deja al consumidor sin protección.
ALTER TABLE company_settings
    ADD COLUMN exchange_period_days INT NOT NULL DEFAULT 7 AFTER reservation_expiration_days;

-- El pedido ya guardaba qué versión del documento aceptó el comprador. Ahora que el
-- plazo lo fija cada empresa y puede cambiarlo cuando quiera, la versión sola no
-- basta para reconstruir qué se prometió: se guarda también el plazo vigente en ese
-- momento. NULL en los pedidos anteriores, que se rigen por el 7 que estaba fijo.
ALTER TABLE orders
    ADD COLUMN terms_exchange_days INT NULL AFTER terms_version;
