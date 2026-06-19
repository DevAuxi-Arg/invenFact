package com.willysoft.productosapi.product.dto;

import com.willysoft.productosapi.category.dto.CategoryResponse;
import java.util.List;

/**
 * Grupo del catálogo: una categoría con sus productos.
 * Usado para el listado con corte de control por categoría.
 */
public record CategoriaProductos(
        CategoryResponse categoria,
        List<ProductResponse> productos
) {
}
