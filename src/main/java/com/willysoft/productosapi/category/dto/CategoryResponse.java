package com.willysoft.productosapi.category.dto;

import java.math.BigDecimal;

public record CategoryResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal alicuotaIva,
        String iconoUrl
) {
}
