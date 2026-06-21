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
        String imagenUrl,
        Integer stockMinimo,
        boolean stockBajo
) {
    /** Compatibilidad: respuesta sin datos de stock mínimo. */
    public ProductResponse(Long id, String nombre, String descripcion, BigDecimal precio, Moneda moneda,
                           Integer stock, CategoryResponse categoria, BigDecimal precioFinalArs,
                           BigDecimal precioFinalUsd, String imagenUrl) {
        this(id, nombre, descripcion, precio, moneda, stock, categoria,
                precioFinalArs, precioFinalUsd, imagenUrl, 0, false);
    }
}
