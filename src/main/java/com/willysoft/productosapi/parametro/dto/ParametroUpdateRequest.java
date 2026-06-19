package com.willysoft.productosapi.parametro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParametroUpdateRequest(
        @NotBlank(message = "El valor es obligatorio")
        @Size(max = 200, message = "El valor no puede exceder 200 caracteres")
        String valor
) {
}
