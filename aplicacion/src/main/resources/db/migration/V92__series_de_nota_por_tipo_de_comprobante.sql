-- La serie de una nota depende de QUÉ comprobante modifica, no del tipo de nota.
--
-- SUNAT exige que una nota sobre factura vaya en serie que empieza por F y una sobre boleta
-- en serie que empieza por B. Cruzarlas es un rechazo por formato (error 1001) que además
-- quema el correlativo. Con un solo campo configurable había que elegir: o se podían
-- acreditar facturas o boletas, nunca las dos, y una tienda emite de los dos tipos.
--
-- Se añaden las series de las notas que modifican una FACTURA. Las columnas que ya existían
-- siguen sirviendo para las notas sobre boleta y como respaldo, de modo que las empresas con
-- Verifac o NubeFact no cambian de comportamiento: NubeFact, además, reutiliza la serie del
-- comprobante de origen y no mira ninguna de estas.
ALTER TABLE billing_configurations
    ADD COLUMN credit_note_invoice_series VARCHAR(10) NULL AFTER credit_note_series,
    ADD COLUMN debit_note_invoice_series VARCHAR(10) NULL AFTER debit_note_series;

-- Si la serie ya configurada es de factura (empieza por F), pertenece a la columna nueva:
-- dejarla donde estaba haría que las notas sobre boleta la usaran y fueran rechazadas.
UPDATE billing_configurations
   SET credit_note_invoice_series = credit_note_series, credit_note_series = NULL
 WHERE credit_note_series LIKE 'F%';

UPDATE billing_configurations
   SET debit_note_invoice_series = debit_note_series, debit_note_series = NULL
 WHERE debit_note_series LIKE 'F%';
