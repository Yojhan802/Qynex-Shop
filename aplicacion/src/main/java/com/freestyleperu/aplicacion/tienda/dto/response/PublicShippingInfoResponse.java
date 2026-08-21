package com.freestyleperu.aplicacion.tienda.dto.response;

import java.math.BigDecimal;

public record PublicShippingInfoResponse(BigDecimal flatRate, String freeShippingDistrict) {
}
