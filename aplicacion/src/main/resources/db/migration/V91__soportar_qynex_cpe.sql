-- Permite seleccionar Qynex CPE como proveedor de facturación por empresa.
--
-- El valor ya existía en el enum de Java, pero la base tiene un CHECK con la lista de
-- proveedores admitidos y no se actualizó: guardar la configuración fallaba con una
-- violación de integridad que el manejador traducía a "conflicto con datos existentes",
-- un mensaje que no describía el problema en absoluto.
--
-- Son dos restricciones y hay que tocar las dos. Con solo la primera, la configuración se
-- guardaría bien y el fallo aparecería después, al emitir el primer comprobante.
--
-- Las empresas existentes conservan su proveedor: esto solo amplía lo que se admite.
ALTER TABLE billing_configurations
    DROP CHECK chk_billing_config_provider;

ALTER TABLE billing_configurations
    ADD CONSTRAINT chk_billing_config_provider
        CHECK (provider IN ('VERIFACT', 'NUBEFACT', 'QYNEX_CPE'));

ALTER TABLE electronic_documents
    DROP CHECK chk_electronic_document_provider;

ALTER TABLE electronic_documents
    ADD CONSTRAINT chk_electronic_document_provider
        CHECK (provider IN ('VERIFACT', 'NUBEFACT', 'QYNEX_CPE'));
