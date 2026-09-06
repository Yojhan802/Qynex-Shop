package com.freestyleperu.aplicacion.facturacion.port;

import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocumentType;

public record ElectronicInvoicingCommand(
        ElectronicDocumentType documentType,
        String series,
        String documentNumber,
        String payloadJson,
        /**
         * Identifica la operación de negocio ante el proveedor, no el intento. Se guarda con
         * el documento para poder reenviar la misma en un reintento: una clave nueva emitiría
         * un segundo comprobante por la misma venta. Nula para los proveedores que no la usan.
         */
        String idempotencyKey) {
}
