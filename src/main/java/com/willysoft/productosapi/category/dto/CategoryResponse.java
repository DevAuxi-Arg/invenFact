package com.willysoft.productosapi.category.dto;

import java.math.BigDecimal;

public record CategoryResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal alicuotaIva,
        String iconoUrl,
        Integer stockMinimo
) {
    /** Compatibilidad: respuesta sin stock mínimo de categoría. */
    public CategoryResponse(Long id, String nombre, String descripcion, BigDecimal alicuotaIva, String iconoUrl) {
        this(id, nombre, descripcion, alicuotaIva, iconoUrl, 0);
    }
}
