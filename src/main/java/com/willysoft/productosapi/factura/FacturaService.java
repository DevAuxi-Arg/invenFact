package com.willysoft.productosapi.factura;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.cliente.Cliente;
import com.willysoft.productosapi.cliente.ClienteService;
import com.willysoft.productosapi.cliente.CondicionIva;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.factura.dto.FacturaCreateRequest;
import com.willysoft.productosapi.factura.dto.FacturaResponse;
import com.willysoft.productosapi.factura.dto.LineaFacturaRequest;
import com.willysoft.productosapi.factura.dto.LineaFacturaResponse;
import com.willysoft.productosapi.parametro.ParametroService;
import com.willysoft.productosapi.product.PriceCalculationService;
import com.willysoft.productosapi.product.Product;
import com.willysoft.productosapi.product.ProductRepository;
import com.willysoft.productosapi.product.dto.PriceBreakdown;
import com.willysoft.productosapi.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ProductRepository productRepository;
    private final ClienteService clienteService;
    private final PriceCalculationService priceCalculationService;
    private final ParametroService parametroService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<FacturaResponse> search(Pageable pageable) {
        return facturaRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public FacturaResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    /**
     * Emite una factura. Congela el precio, el tipo de cambio y el IVA de cada línea
     * (snapshot) y descuenta el stock de los productos vendidos.
     */
    /** Largo máximo de observaciones (debe coincidir con la columna de la entidad Factura). */
    private static final int MAX_OBSERVACIONES = 500;

    @Transactional
    public FacturaResponse create(FacturaCreateRequest request) {
        if (request.observaciones() != null && request.observaciones().length() > MAX_OBSERVACIONES) {
            throw new ConflictException("Las observaciones no pueden superar los " + MAX_OBSERVACIONES
                    + " caracteres (tenés " + request.observaciones().length() + ").");
        }
        BigDecimal dolar = parametroService.getDolar();
        ClienteResuelto cliente = resolverCliente(request);
        boolean exento = cliente.condicion() == CondicionIva.EXENTO;

        Factura factura = Factura.builder()
                .fecha(LocalDateTime.now())
                .clienteId(cliente.clienteId())
                .clienteNombre(cliente.nombre())
                .clienteDocumento(cliente.documento())
                .condicionIvaCliente(cliente.condicion())
                .observaciones(request.observaciones())
                .tipoCambioAplicado(dolar)
                .estado(EstadoFactura.EMITIDA)
                .creadoPor(SecurityUtils.currentEmail())
                .subtotalNeto(BigDecimal.ZERO)
                .totalIva(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .build();

        BigDecimal subtotalNeto = BigDecimal.ZERO;
        BigDecimal totalIva = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        // Validación previa: junta TODOS los faltantes de stock para informarlos juntos
        // (así el mensaje coincide con las líneas que se resaltan en rojo en el form).
        List<String> faltantes = new ArrayList<>();
        for (LineaFacturaRequest l : request.lineas()) {
            Product producto = productRepository.findById(l.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + l.productoId()));
            if (producto.getStock() < l.cantidad()) {
                faltantes.add(producto.getNombre()
                        + " (disponible: " + producto.getStock() + ", pedido: " + l.cantidad() + ")");
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ConflictException("Stock insuficiente para " + String.join("; ", faltantes));
        }

        for (LineaFacturaRequest l : request.lineas()) {
            Product producto = productRepository.findById(l.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + l.productoId()));

            // Cálculo por unidad con el dólar de la emisión, luego × cantidad.
            // Si el cliente es exento, la venta no se grava (IVA = 0).
            PriceBreakdown unidad = priceCalculationService.calcular(producto, dolar);
            BigDecimal cantidad = BigDecimal.valueOf(l.cantidad());
            BigDecimal lineaNeto = unidad.netoArs().multiply(cantidad);
            BigDecimal lineaIva = exento ? BigDecimal.ZERO : unidad.ivaMontoArs().multiply(cantidad);
            BigDecimal lineaTotal = lineaNeto.add(lineaIva);

            factura.addLinea(LineaFactura.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .monedaOriginal(unidad.monedaOriginal())
                    .precioUnitarioOriginal(producto.getPrecio())
                    .cantidad(l.cantidad())
                    .tipoCambioAplicado(dolar)
                    .alicuotaIva(exento ? BigDecimal.ZERO : unidad.alicuotaIva())
                    .netoArs(lineaNeto)
                    .ivaMontoArs(lineaIva)
                    .totalArs(lineaTotal)
                    .build());

            // Descuenta stock del producto vendido.
            producto.setStock(producto.getStock() - l.cantidad());
            productRepository.save(producto);

            subtotalNeto = subtotalNeto.add(lineaNeto);
            totalIva = totalIva.add(lineaIva);
            total = total.add(lineaTotal);
        }

        factura.setSubtotalNeto(subtotalNeto);
        factura.setTotalIva(totalIva);
        factura.setTotal(total);

        Factura guardada = facturaRepository.save(factura);
        guardada.setNumero(String.format("F-%08d", guardada.getId()));
        guardada = facturaRepository.save(guardada);

        auditService.log("FACTURA_EMITIDA",
                guardada.getNumero() + " · " + guardada.getClienteNombre() + " · total " + total,
                SecurityUtils.currentEmail());
        return toResponse(guardada);
    }

    /** Anula una factura y devuelve el stock de sus líneas. */
    @Transactional
    public FacturaResponse anular(Long id) {
        Factura factura = getOrThrow(id);
        if (factura.getEstado() == EstadoFactura.ANULADA) {
            throw new ConflictException("La factura ya está anulada");
        }
        factura.setEstado(EstadoFactura.ANULADA);
        for (LineaFactura linea : factura.getLineas()) {
            if (linea.getProductoId() != null) {
                productRepository.findById(linea.getProductoId()).ifPresent(p -> {
                    p.setStock(p.getStock() + linea.getCantidad());
                    productRepository.save(p);
                });
            }
        }
        auditService.log("FACTURA_ANULADA", factura.getNumero(), SecurityUtils.currentEmail());
        return toResponse(factura);
    }

    private Factura getOrThrow(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura no encontrada con id: " + id));
    }

    /** Datos del cliente a usar en la factura, vengan de uno existente o de carga libre. */
    private record ClienteResuelto(Long clienteId, String nombre, String documento, CondicionIva condicion) {
    }

    private ClienteResuelto resolverCliente(FacturaCreateRequest request) {
        if (request.clienteId() != null) {
            Cliente c = clienteService.getEntityOrThrow(request.clienteId());
            return new ClienteResuelto(c.getId(), c.getNombre(), c.getDocumento(), c.getCondicionIva());
        }
        if (!StringUtils.hasText(request.clienteNombre())) {
            throw new ConflictException("Indicá un cliente: elegí uno existente o cargá el nombre");
        }
        CondicionIva condicion = request.condicionIva() != null
                ? request.condicionIva() : CondicionIva.CONSUMIDOR_FINAL;
        Long clienteId = null;
        if (request.registrarCliente()) {
            // Alta en la base del cliente nuevo cargado en la factura.
            Cliente nuevo = clienteService.crearRapido(
                    request.clienteNombre(), request.clienteDocumento(), condicion);
            clienteId = nuevo.getId();
        }
        return new ClienteResuelto(clienteId, request.clienteNombre(), request.clienteDocumento(), condicion);
    }

    private FacturaResponse toResponse(Factura f) {
        var lineas = f.getLineas().stream()
                .map(l -> new LineaFacturaResponse(
                        l.getProductoId(),
                        l.getNombreProducto(),
                        l.getMonedaOriginal(),
                        l.getPrecioUnitarioOriginal(),
                        l.getCantidad(),
                        l.getAlicuotaIva(),
                        l.getNetoArs(),
                        l.getIvaMontoArs(),
                        l.getTotalArs()))
                .toList();
        return new FacturaResponse(
                f.getId(),
                f.getNumero(),
                f.getFecha(),
                f.getClienteId(),
                f.getClienteNombre(),
                f.getClienteDocumento(),
                f.getCondicionIvaCliente(),
                f.getObservaciones(),
                f.getTipoCambioAplicado(),
                f.getSubtotalNeto(),
                f.getTotalIva(),
                f.getTotal(),
                f.getEstado(),
                f.getCreadoPor(),
                lineas);
    }
}
