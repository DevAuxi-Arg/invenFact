package com.willysoft.productosapi.factura;

import com.willysoft.productosapi.factura.dto.ClienteTop;
import com.willysoft.productosapi.factura.dto.ProductoVendido;
import com.willysoft.productosapi.factura.dto.VentasDashboard;
import com.willysoft.productosapi.factura.dto.VentasResumen;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VentasService {

    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final int MESES = 6;
    private static final int TOP = 5;

    private final FacturaRepository facturaRepository;

    @Transactional(readOnly = true)
    public VentasDashboard dashboard() {
        List<Object[]> filas = facturaRepository.findFechaTotalIva(EstadoFactura.EMITIDA);

        BigDecimal totalFacturado = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal ventasMesActual = BigDecimal.ZERO;
        YearMonth mesActual = YearMonth.now();
        Map<YearMonth, BigDecimal> porMes = new HashMap<>();

        for (Object[] fila : filas) {
            LocalDateTime fecha = (LocalDateTime) fila[0];
            BigDecimal total = (BigDecimal) fila[1];
            BigDecimal iva = (BigDecimal) fila[2];

            totalFacturado = totalFacturado.add(total);
            totalIva = totalIva.add(iva);

            YearMonth ym = YearMonth.from(fecha);
            porMes.merge(ym, total, BigDecimal::add);
            if (ym.equals(mesActual)) {
                ventasMesActual = ventasMesActual.add(total);
            }
        }

        long cantidad = filas.size();
        BigDecimal ticket = cantidad > 0
                ? totalFacturado.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        long anuladas = facturaRepository.countByEstado(EstadoFactura.ANULADA);

        // Serie de los últimos MESES meses (rellena con 0 los meses sin ventas).
        List<String> labels = new ArrayList<>();
        List<BigDecimal> data = new ArrayList<>();
        for (int i = MESES - 1; i >= 0; i--) {
            YearMonth ym = mesActual.minusMonths(i);
            labels.add(ym.format(MES_FMT));
            data.add(porMes.getOrDefault(ym, BigDecimal.ZERO));
        }

        VentasResumen resumen = new VentasResumen(
                totalFacturado, cantidad, ticket, totalIva, anuladas, ventasMesActual);

        List<ProductoVendido> topProductos =
                facturaRepository.topProductos(EstadoFactura.EMITIDA, PageRequest.of(0, TOP));
        List<ClienteTop> topClientes =
                facturaRepository.topClientes(EstadoFactura.EMITIDA, PageRequest.of(0, TOP));

        return new VentasDashboard(resumen, labels, data, topProductos, topClientes);
    }
}
