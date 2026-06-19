package com.willysoft.productosapi.factura;

import com.willysoft.productosapi.product.Moneda;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Renglón de una factura. Todos los datos del producto y del cálculo están
 * <b>copiados</b> (snapshot): si el producto cambia o se borra, la línea no se altera.
 */
@Entity
@Table(name = "lineas_factura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    /** Referencia al producto (puede dejar de existir; los datos quedan en la línea). */
    private Long productoId;

    @Column(nullable = false, length = 150)
    private String nombreProducto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Moneda monedaOriginal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioOriginal;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal tipoCambioAplicado;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal netoArs;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal ivaMontoArs;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalArs;
}
