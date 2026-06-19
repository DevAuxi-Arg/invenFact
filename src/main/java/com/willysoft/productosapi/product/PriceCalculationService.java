package com.willysoft.productosapi.product;

import com.willysoft.productosapi.parametro.ParametroService;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Calcula el precio final de un producto aplicando el IVA de su categoría y,
 * si corresponde, convirtiendo de USD a ARS con el dólar vigente.
 *
 * <p>Reglas: la moneda base es ARS; todos los montos en pesos se redondean a 2
 * decimales con {@link RoundingMode#HALF_UP}. El precio en USD se deriva del precio
 * final en ARS dividiendo por el dólar vigente.</p>
 */
@Service
@RequiredArgsConstructor
public class PriceCalculationService {

    private static final int ESCALA = 2;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final ParametroService parametroService;

    public PriceBreakdown calcular(Product producto) {
        return calcular(producto, parametroService.getDolar());
    }

    /** Variante con el dólar ya resuelto (útil para listar muchos productos sin releer el parámetro). */
    public PriceBreakdown calcular(Product producto, BigDecimal dolar) {
        Moneda moneda = producto.getMoneda() != null ? producto.getMoneda() : Moneda.ARS;
        BigDecimal precio = producto.getPrecio();
        BigDecimal alicuota = producto.getCategoria().getAlicuotaIva();
        if (alicuota == null) {
            alicuota = BigDecimal.ZERO;
        }

        // 1) Neto en pesos
        BigDecimal netoArs = (moneda == Moneda.USD)
                ? precio.multiply(dolar)
                : precio;
        netoArs = netoArs.setScale(ESCALA, RoundingMode.HALF_UP);

        // 2) IVA
        BigDecimal ivaMontoArs = netoArs.multiply(alicuota)
                .divide(CIEN, ESCALA, RoundingMode.HALF_UP);

        // 3) Precio final en ARS y su equivalente en USD
        BigDecimal precioFinalArs = netoArs.add(ivaMontoArs);
        BigDecimal precioFinalUsd = (dolar.signum() > 0)
                ? precioFinalArs.divide(dolar, ESCALA, RoundingMode.HALF_UP)
                : null;

        return new PriceBreakdown(
                moneda,
                precio,
                dolar,
                netoArs,
                alicuota,
                ivaMontoArs,
                precioFinalArs,
                precioFinalUsd
        );
    }
}
