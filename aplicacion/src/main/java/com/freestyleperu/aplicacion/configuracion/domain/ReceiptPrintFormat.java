package com.freestyleperu.aplicacion.configuracion.domain;

/**
 * Formato en el que se imprime la representación impresa del comprobante.
 *
 * <p>Los dos los genera Qynex CPE con el mismo contenido legal —cambia el papel, no lo que
 * dice—, así que elegir uno u otro no afecta a la validez del comprobante.
 */
public enum ReceiptPrintFormat {
    /** Rollo de 80 mm. Lo normal en caja, con impresora térmica. */
    TICKET,
    /** Hoja A4, para tiendas que no tienen térmica. */
    A4
}
