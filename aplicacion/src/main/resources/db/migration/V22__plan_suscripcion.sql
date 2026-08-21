-- Plan de suscripción de esta instalación (modelo SaaS de un despliegue por
-- cliente, ver docs/03-modelo-datos.md). No es editable por el cliente desde
-- la API: solo se cambia directo en la base de datos por el operador de la
-- plataforma. La instalación existente se marca ECOMMERCE porque ya tiene
-- construida y en uso la tienda online.

ALTER TABLE company_settings
    ADD COLUMN plan VARCHAR(20) NOT NULL DEFAULT 'ECOMMERCE',
    ADD CONSTRAINT chk_company_settings_plan CHECK (plan IN ('STARTER', 'PROFESIONAL', 'ECOMMERCE', 'IA'));
