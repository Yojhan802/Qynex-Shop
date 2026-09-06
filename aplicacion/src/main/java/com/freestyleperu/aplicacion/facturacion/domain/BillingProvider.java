package com.freestyleperu.aplicacion.facturacion.domain;

public enum BillingProvider {
    VERIFACT,
    NUBEFACT,
    /**
     * Motor propio de emisión a SUNAT. La empresa aporta su certificado y su Clave SOL:
     * Qynex opera la infraestructura, pero no firma ni se autentica ante SUNAT con
     * credenciales propias, así que no es PSE ni OSE.
     */
    QYNEX_CPE
}
