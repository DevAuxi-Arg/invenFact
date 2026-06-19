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
 * decimales con {@link RoundingMode#HALF_UP}. Solo los productos cargados en USD
 * llevan precio en dólares (se deriva del final en ARS dividiendo por el dólar
 * vigente); los productos en pesos dejan el precio en USD en {@code null}.</p>
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

        // 3) Precio final en ARS. El precio en USD solo se muestra si el producto
        //    fue cargado en dólares (los productos en pesos no llevan precio en USD).
        BigDecimal precioFinalArs = netoArs.add(ivaMontoArs);
        BigDecimal precioFinalUsd = (moneda == Moneda.USD && dolar.signum() > 0)
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
