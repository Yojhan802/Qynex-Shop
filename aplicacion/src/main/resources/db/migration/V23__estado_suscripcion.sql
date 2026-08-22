-- Estado de pago de la suscripción de esta instalación (SaaS de un despliegue
-- por cliente, ver docs/03-modelo-datos.md §15). next_payment_due es la fecha
-- del próximo cobro; un job programado (SuscripcionScheduler) marca
-- SUSPENDIDA automáticamente si se pasa esa fecha más un margen de gracia.
-- Igual que plan: no editable por el cliente desde la API, solo por el
-- operador de la plataforma directo en la base de datos.

ALTER TABLE company_settings
    ADD COLUMN subscription_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    ADD COLUMN next_payment_due DATE NULL,
    ADD CONSTRAINT chk_company_settings_subscription_status CHECK (subscription_status IN ('ACTIVA', 'SUSPENDIDA'));
