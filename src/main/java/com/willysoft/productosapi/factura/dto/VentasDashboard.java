package com.willysoft.productosapi.factura.dto;

import java.math.BigDecimal;
import java.util.List;

/** Datos completos del dashboard de ventas. */
public record VentasDashboard(
        VentasResumen resumen,
        List<String> mesesLabels,
        List<BigDecimal> mesesData,
        List<ProductoVendido> topProductos,
        List<ClienteTop> topClientes
) {
}
