package com.freestyleperu.aplicacion.configuracion.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Solo datos operativos (moneda, IGV, envío, pie de ticket, separaciones) —
 * la identidad de la empresa (razón social, RUC, dirección, contacto, logo)
 * vive aparte en {@code ActualizarIdentidadEmpresaRequest}, reservada al
 * operador de la plataforma (RN-26).
 */
public record ActualizarCompanySettingsRequest(
        @NotBlank @Size(max = 3) String currencyCode,
        @NotBlank @Size(max = 5) String currencySymbol,
        @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal igvRate,
        @Size(max = 255) String ticketFooter,
        @NotNull @DecimalMin("0") BigDecimal shippingFlatRate,
        @NotNull @DecimalMin("0") BigDecimal reservationDepositAmount,
        @NotNull @Min(1) Integer reservationExpirationDays,
        /** Días de cambio voluntario; 0 = el negocio no lo ofrece. Nulo deja el valor actual. */
        @Min(0) @Max(365) Integer exchangePeriodDays,
        Boolean onlinePaymentsEnabled,
        Boolean electronicInvoicingEnabled,
        /** TICKET (80 mm) o A4. Nulo conserva el que ya tuviera la empresa. */
        com.freestyleperu.aplicacion.configuracion.domain.ReceiptPrintFormat receiptPrintFormat) {

    /** Compatibilidad con llamadas internas y tests que aún no envían los flags opcionales. */
    public ActualizarCompanySettingsRequest(
            String currencyCode,
            String currencySymbol,
            BigDecimal igvRate,
            String ticketFooter,
            BigDecimal shippingFlatRate,
                BigDecimal reservationDepositAmount,
                Integer reservationExpirationDays) {
        this(currencyCode, currencySymbol, igvRate, ticketFooter, shippingFlatRate,
                reservationDepositAmount, reservationExpirationDays, null, null, null, null);
    }
}
