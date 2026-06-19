package com.willysoft.productosapi.factura.dto;

import java.math.BigDecimal;

/** Producto agregado por ventas (para el ranking del dashboard). */
public record ProductoVendido(
        String nombre,
        Long cantidad,
        BigDecimal total
) {
}
