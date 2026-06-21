package com.willysoft.productosapi.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.category.Category;
import com.willysoft.productosapi.parametro.ParametroService;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceCalculationServiceTest {

    @Mock
    private ParametroService parametroService;

    @InjectMocks
    private PriceCalculationService service;

    private Product producto(BigDecimal precio, Moneda moneda, BigDecimal alicuota) {
        Category cat = Category.builder().id(1L).nombre("Cat").alicuotaIva(alicuota).build();
        return Product.builder().id(1L).nombre("P").precio(precio).moneda(moneda).stock(1).categoria(cat).build();
    }

    @Test
    void producto_en_pesos_con_iva_21() {
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1000.00"));
        PriceBreakdown b = service.calcular(producto(new BigDecimal("100.00"), Moneda.ARS, new BigDecimal("21.00")));

        assertThat(b.netoArs()).isEqualByComparingTo("100.00");
        assertThat(b.ivaMontoArs()).isEqualByComparingTo("21.00");
        assertThat(b.precioFinalArs()).isEqualByComparingTo("121.00");
        // Un producto cargado en pesos no lleva precio en USD (queda null).
        assertThat(b.precioFinalUsd()).isNull();
    }

    @Test
    void producto_en_dolares_convierte_y_aplica_iva() {
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1000.00"));
        PriceBreakdown b = service.calcular(producto(new BigDecimal("100.00"), Moneda.USD, new BigDecimal("10.50")));

        assertThat(b.netoArs()).isEqualByComparingTo("100000.00");   // 100 * 1000
        assertThat(b.ivaMontoArs()).isEqualByComparingTo("10500.00"); // 10.5%
        assertThat(b.precioFinalArs()).isEqualByComparingTo("110500.00");
        assertThat(b.precioFinalUsd()).isEqualByComparingTo("110.50"); // 110500 / 1000
    }

    @Test
    void producto_exento_no_suma_iva() {
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1000.00"));
        PriceBreakdown b = service.calcular(producto(new BigDecimal("50.00"), Moneda.ARS, new BigDecimal("0.00")));

        assertThat(b.ivaMontoArs()).isEqualByComparingTo("0.00");
        assertThat(b.precioFinalArs()).isEqualByComparingTo("50.00");
    }

    @Test
    void redondeo_half_up_a_dos_decimales() {
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1.00"));
        // 33.33 * 21% = 6.9993 -> 7.00
        PriceBreakdown b = service.calcular(producto(new BigDecimal("33.33"), Moneda.ARS, new BigDecimal("21.00")));

        assertThat(b.ivaMontoArs()).isEqualByComparingTo("7.00");
        assertThat(b.precioFinalArs()).isEqualByComparingTo("40.33");
    }
}
