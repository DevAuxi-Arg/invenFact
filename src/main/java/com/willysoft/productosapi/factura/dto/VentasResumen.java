package com.willysoft.productosapi.factura.dto;

import java.math.BigDecimal;

/** KPIs del dashboard de ventas (sobre facturas EMITIDAS). */
public record VentasResumen(
        BigDecimal totalFacturado,
        long cantidadFacturas,
        BigDecimal ticketPromedio,
        BigDecimal totalIva,
        long facturasAnuladas,
        BigDecimal ventasMesActual
) {
}
