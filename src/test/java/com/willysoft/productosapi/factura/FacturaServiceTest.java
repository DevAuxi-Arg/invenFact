package com.willysoft.productosapi.factura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.category.Category;
import com.willysoft.productosapi.cliente.ClienteService;
import com.willysoft.productosapi.cliente.CondicionIva;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.factura.dto.FacturaCreateRequest;
import com.willysoft.productosapi.factura.dto.FacturaResponse;
import com.willysoft.productosapi.factura.dto.LineaFacturaRequest;
import com.willysoft.productosapi.parametro.ParametroService;
import com.willysoft.productosapi.product.Moneda;
import com.willysoft.productosapi.product.PriceCalculationService;
import com.willysoft.productosapi.product.Product;
import com.willysoft.productosapi.product.ProductRepository;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PriceCalculationService priceCalculationService;
    @Mock
    private ParametroService parametroService;
    @Mock
    private ClienteService clienteService;
    @Mock
    private com.willysoft.productosapi.audit.AuditService auditService;

    @InjectMocks
    private FacturaService service;

    private Product producto(int stock) {
        Category cat = Category.builder().id(1L).nombre("Cat").alicuotaIva(new BigDecimal("21.00")).build();
        return Product.builder().id(1L).nombre("Notebook").precio(new BigDecimal("100.00"))
                .moneda(Moneda.ARS).stock(stock).categoria(cat).build();
    }

    private PriceBreakdown breakdownPorUnidad() {
        // neto 100, IVA 21 (21%), final 121 por unidad
        return new PriceBreakdown(Moneda.ARS, new BigDecimal("100.00"), new BigDecimal("1000.00"),
                new BigDecimal("100.00"), new BigDecimal("21.00"), new BigDecimal("21.00"),
                new BigDecimal("121.00"), new BigDecimal("0.12"));
    }

    @Test
    void emite_congela_valores_y_descuenta_stock() {
        Product p = producto(10);
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1000.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(priceCalculationService.calcular(any(Product.class), any(BigDecimal.class)))
                .thenReturn(breakdownPorUnidad());
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(5L);
            }
            return f;
        });

        var req = new FacturaCreateRequest(null, "Juan", "20-1234-5", null, false, null,
                List.of(new LineaFacturaRequest(1L, 2)));
        FacturaResponse resp = service.create(req);

        // Snapshot: 2 unidades × (neto 100 + IVA 21)
        assertThat(resp.subtotalNeto()).isEqualByComparingTo("200.00");
        assertThat(resp.totalIva()).isEqualByComparingTo("42.00");
        assertThat(resp.total()).isEqualByComparingTo("242.00");
        assertThat(resp.tipoCambioAplicado()).isEqualByComparingTo("1000.00");
        assertThat(resp.numero()).isEqualTo("F-00000005");
        assertThat(resp.lineas()).hasSize(1);
        assertThat(resp.lineas().get(0).totalArs()).isEqualByComparingTo("242.00");

        // Stock descontado: 10 - 2 = 8
        assertThat(p.getStock()).isEqualTo(8);
        verify(productRepository).save(p);
    }

    @Test
    void cliente_exento_no_cobra_iva() {
        Product p = producto(10);
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1000.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));
        when(priceCalculationService.calcular(any(Product.class), any(BigDecimal.class)))
                .thenReturn(breakdownPorUnidad());
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> {
            Factura f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(7L);
            }
            return f;
        });

        var req = new FacturaCreateRequest(null, "ONG Exenta", null, CondicionIva.EXENTO, false, null,
                List.of(new LineaFacturaRequest(1L, 2)));
        FacturaResponse resp = service.create(req);

        assertThat(resp.condicionIvaCliente()).isEqualTo(CondicionIva.EXENTO);
        assertThat(resp.totalIva()).isEqualByComparingTo("0.00");
        assertThat(resp.total()).isEqualByComparingTo("200.00"); // solo neto, sin IVA
    }

    @Test
    void rechaza_si_stock_insuficiente() {
        Product p = producto(1);
        when(parametroService.getDolar()).thenReturn(new BigDecimal("1000.00"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        var req = new FacturaCreateRequest(null, "Juan", null, null, false, null,
                List.of(new LineaFacturaRequest(1L, 5)));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(facturaRepository, never()).save(any());
    }

    @Test
    void anular_restituye_stock_y_marca_anulada() {
        Product p = producto(8);
        LineaFactura linea = LineaFactura.builder()
                .productoId(1L).nombreProducto("Notebook").monedaOriginal(Moneda.ARS)
                .precioUnitarioOriginal(new BigDecimal("100.00")).cantidad(3)
                .tipoCambioAplicado(new BigDecimal("1000.00")).alicuotaIva(new BigDecimal("21.00"))
                .netoArs(new BigDecimal("300.00")).ivaMontoArs(new BigDecimal("63.00"))
                .totalArs(new BigDecimal("363.00")).build();
        Factura factura = Factura.builder().id(1L).numero("F-00000001").fecha(LocalDateTime.now())
                .clienteNombre("Juan").tipoCambioAplicado(new BigDecimal("1000.00"))
                .subtotalNeto(new BigDecimal("300.00")).totalIva(new BigDecimal("63.00"))
                .total(new BigDecimal("363.00")).estado(EstadoFactura.EMITIDA)
                .lineas(new ArrayList<>(List.of(linea))).build();

        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        FacturaResponse resp = service.anular(1L);

        assertThat(resp.estado()).isEqualTo(EstadoFactura.ANULADA);
        assertThat(p.getStock()).isEqualTo(11); // 8 + 3
        verify(productRepository).save(p);
    }

    @Test
    void anular_falla_si_ya_anulada() {
        Factura factura = Factura.builder().id(1L).estado(EstadoFactura.ANULADA)
                .lineas(new ArrayList<>()).build();
        when(facturaRepository.findById(1L)).thenReturn(Optional.of(factura));

        assertThatThrownBy(() -> service.anular(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya está anulada");
    }
}
