package com.willysoft.productosapi.factura.dto;

import com.willysoft.productosapi.cliente.CondicionIva;
import com.willysoft.productosapi.factura.EstadoFactura;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FacturaResponse(
        Long id,
        String numero,
        LocalDateTime fecha,
        Long clienteId,
        String clienteNombre,
        String clienteDocumento,
        CondicionIva condicionIvaCliente,
        String observaciones,
        BigDecimal tipoCambioAplicado,
        BigDecimal subtotalNeto,
        BigDecimal totalIva,
        BigDecimal total,
        EstadoFactura estado,
        String creadoPor,
        List<LineaFacturaResponse> lineas
) {
}
