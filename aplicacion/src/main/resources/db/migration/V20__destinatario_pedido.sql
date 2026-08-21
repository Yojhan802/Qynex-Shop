-- Shalom (la courier que usa la empresa) exige DNI y apellidos separados
-- (paterno/materno) del destinatario para poder registrar el envío — un
-- nombre completo libre no alcanza. Se reemplaza recipient_name por estos
-- campos estructurados.

ALTER TABLE orders
    DROP COLUMN recipient_name,
    ADD COLUMN recipient_dni VARCHAR(15) NOT NULL DEFAULT '',
    ADD COLUMN recipient_first_name VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN recipient_last_name_paterno VARCHAR(60) NOT NULL DEFAULT '',
    ADD COLUMN recipient_last_name_materno VARCHAR(60) NOT NULL DEFAULT '';
