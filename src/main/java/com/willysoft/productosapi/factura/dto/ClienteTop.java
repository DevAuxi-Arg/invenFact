package com.willysoft.productosapi.factura.dto;

import java.math.BigDecimal;

/** Cliente agregado por facturación (para el ranking del dashboard). */
public record ClienteTop(
        String nombre,
        Long cantidadFacturas,
        BigDecimal total
) {
}
