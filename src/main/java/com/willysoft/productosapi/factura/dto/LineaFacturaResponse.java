package com.willysoft.productosapi.factura.dto;

import com.willysoft.productosapi.product.Moneda;
import java.math.BigDecimal;

public record LineaFacturaResponse(
        Long productoId,
        String nombreProducto,
        Moneda monedaOriginal,
        BigDecimal precioUnitarioOriginal,
        Integer cantidad,
        BigDecimal alicuotaIva,
        BigDecimal netoArs,
        BigDecimal ivaMontoArs,
        BigDecimal totalArs
) {
}
