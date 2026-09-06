-- Formato en el que se imprime la representación impresa del comprobante electrónico.
--
-- No todas las tiendas tienen impresora térmica: una tienda pequeña imprime en una hoja
-- normal, y obligarla al rollo de 80 mm le deja el comprobante cortado o ilegible.
--
-- TICKET es el valor por defecto porque es lo habitual en retail y lo que espera un cliente
-- en caja. El formato se lo pide Qynex CPE, que es quien genera la representación impresa
-- con el QR del Anexo N.º 6: aquí solo se guarda cuál de los dos quiere cada empresa.
ALTER TABLE company_settings
    ADD COLUMN receipt_print_format VARCHAR(10) NOT NULL DEFAULT 'TICKET' AFTER ticket_footer;

ALTER TABLE company_settings
    ADD CONSTRAINT chk_company_receipt_print_format
        CHECK (receipt_print_format IN ('TICKET', 'A4'));
