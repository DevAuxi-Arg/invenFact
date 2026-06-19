package com.willysoft.productosapi.factura;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comprobante histórico de una venta. Sus importes y el tipo de cambio quedan
 * <b>congelados</b> al emitirse: no se recalculan aunque luego cambien el dólar,
 * el IVA de la categoría o el precio del producto.
 */
@Entity
@Table(name = "facturas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número de comprobante legible (ej. F-00000001). Se asigna al emitir. */
    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    /** Referencia suelta al cliente registrado (puede ser null si fue carga libre). */
    private Long clienteId;

    @Column(nullable = false, length = 150)
    private String clienteNombre;

    @Column(length = 50)
    private String clienteDocumento;

    /** Condición de IVA del cliente al momento de emitir (snapshot). */
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(length = 25)
    private com.willysoft.productosapi.cliente.CondicionIva condicionIvaCliente;

    @Column(length = 500)
    private String observaciones;

    /** Cotización del dólar usada al emitir (foto). */
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal tipoCambioAplicado;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotalNeto;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalIva;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoFactura estado;

    @Column(length = 150)
    private String creadoPor;

    @Builder.Default
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineaFactura> lineas = new ArrayList<>();

    public void addLinea(LineaFactura linea) {
        linea.setFactura(this);
        this.lineas.add(linea);
    }
}
