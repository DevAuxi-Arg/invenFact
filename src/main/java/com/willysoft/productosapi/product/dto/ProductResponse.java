package com.willysoft.productosapi.product.dto;

import com.willysoft.productosapi.category.dto.CategoryResponse;
import com.willysoft.productosapi.product.Moneda;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Moneda moneda,
        Integer stock,
        CategoryResponse categoria,
        BigDecimal precioFinalArs,
        BigDecimal precioFinalUsd,
        String imagenUrl
) {
}
