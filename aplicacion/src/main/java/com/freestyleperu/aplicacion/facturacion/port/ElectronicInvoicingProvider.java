package com.freestyleperu.aplicacion.facturacion.port;

import com.freestyleperu.aplicacion.facturacion.domain.BillingProvider;

public interface ElectronicInvoicingProvider {

    BillingProvider type();

    ElectronicInvoicingResult issue(
            ElectronicInvoicingCommand command, BillingConfigurationData configuration);

    ElectronicInvoicingResult fetchStatus(String providerDocumentId, BillingConfigurationData configuration);

    ElectronicInvoicingResult retry(String providerDocumentId, BillingConfigurationData configuration);

    /** Permite a proveedores que necesitan el comprobante completo para reintentar el envío. */
    default ElectronicInvoicingResult retry(
            ElectronicInvoicingCommand command, String providerDocumentId,
            BillingConfigurationData configuration) {
        return retry(providerDocumentId, configuration);
    }

    ElectronicInvoicingResource download(
            String providerDocumentId, String resource, BillingConfigurationData configuration);

    /**
     * Series que la empresa puede usar, si el proveedor sabe decirlo.
     *
     * <p>Por defecto vacía: no todos lo publican, y para esos el panel sigue pidiendo la
     * serie como texto. Devolver la lista convierte ese campo libre en una elección entre
     * lo que existe de verdad, que es donde se cazan las erratas — una serie mal escrita no
     * falla al guardar, falla al emitir.
     */
    default java.util.List<SerieDisponible> series(BillingConfigurationData configuration) {
        return java.util.List.of();
    }

    /**
     * @param documentType     código del catálogo 01 de SUNAT: 01 factura, 03 boleta, 07 y 08 notas
     * @param documentTypeName nombre legible, para pintar la lista sin traducir códigos
     * @param series           la serie, que es lo que se guarda y se manda al emitir
     */
    record SerieDisponible(String documentType, String documentTypeName, String series) {
    }
}
