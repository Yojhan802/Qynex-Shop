package com.freestyleperu.aplicacion.combo.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/** El precio del combo debe ser menor a la suma de sus productos por separado — se valida en el servicio. */
public record ComboRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @NotEmpty @Valid List<ComboItemRequest> items) {
}
