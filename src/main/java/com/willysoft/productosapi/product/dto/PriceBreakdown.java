package com.willysoft.productosapi.product.dto;

import com.willysoft.productosapi.product.Moneda;
import java.math.BigDecimal;

/**
 * Desglose del precio de un producto, calculado al momento (no persistido).
 * La fuente de verdad es ARS; {@code precioFinalUsd} se deriva con el dólar vigente.
 */
public record PriceBreakdown(
        Moneda monedaOriginal,
        BigDecimal precioOriginal,
        BigDecimal tipoCambioAplicado,
        BigDecimal netoArs,
        BigDecimal alicuotaIva,
        BigDecimal ivaMontoArs,
        BigDecimal precioFinalArs,
        BigDecimal precioFinalUsd
) {
}
