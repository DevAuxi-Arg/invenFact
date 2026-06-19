package com.willysoft.productosapi.factura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.factura.dto.VentasDashboard;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class VentasServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @InjectMocks
    private VentasService service;

    @Test
    void calcula_kpis_y_serie_de_meses() {
        LocalDateTime ahora = LocalDateTime.now();
        // 2 facturas en el mes actual: totales 100 y 300, IVA 21 y 63
        when(facturaRepository.findFechaTotalIva(EstadoFactura.EMITIDA)).thenReturn(List.of(
                new Object[]{ahora, new BigDecimal("100.00"), new BigDecimal("21.00")},
                new Object[]{ahora, new BigDecimal("300.00"), new BigDecimal("63.00")}
        ));
        when(facturaRepository.countByEstado(EstadoFactura.ANULADA)).thenReturn(1L);
        when(facturaRepository.topProductos(eq(EstadoFactura.EMITIDA), any(Pageable.class)))
                .thenReturn(List.of());
        when(facturaRepository.topClientes(eq(EstadoFactura.EMITIDA), any(Pageable.class)))
                .thenReturn(List.of());

        VentasDashboard d = service.dashboard();

        assertThat(d.resumen().totalFacturado()).isEqualByComparingTo("400.00");
        assertThat(d.resumen().cantidadFacturas()).isEqualTo(2);
        assertThat(d.resumen().ticketPromedio()).isEqualByComparingTo("200.00");
        assertThat(d.resumen().totalIva()).isEqualByComparingTo("84.00");
        assertThat(d.resumen().facturasAnuladas()).isEqualTo(1);
        assertThat(d.resumen().ventasMesActual()).isEqualByComparingTo("400.00");

        // serie de 6 meses; el último corresponde al mes actual con 400
        assertThat(d.mesesLabels()).hasSize(6);
        assertThat(d.mesesData()).hasSize(6);
        assertThat(d.mesesLabels().get(5)).isEqualTo(YearMonth.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")));
        assertThat(d.mesesData().get(5)).isEqualByComparingTo("400.00");
    }

    @Test
    void sin_facturas_devuelve_ceros() {
        when(facturaRepository.findFechaTotalIva(EstadoFactura.EMITIDA)).thenReturn(List.of());
        when(facturaRepository.countByEstado(EstadoFactura.ANULADA)).thenReturn(0L);
        when(facturaRepository.topProductos(eq(EstadoFactura.EMITIDA), any(Pageable.class)))
                .thenReturn(List.of());
        when(facturaRepository.topClientes(eq(EstadoFactura.EMITIDA), any(Pageable.class)))
                .thenReturn(List.of());

        VentasDashboard d = service.dashboard();

        assertThat(d.resumen().cantidadFacturas()).isZero();
        assertThat(d.resumen().totalFacturado()).isEqualByComparingTo("0");
        assertThat(d.resumen().ticketPromedio()).isEqualByComparingTo("0");
    }
}
