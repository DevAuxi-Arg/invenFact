package com.willysoft.productosapi.product;

import com.willysoft.productosapi.category.Category;
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

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** URL de la imagen del producto (opcional). */
    @Column(name = "imagen_url", columnDefinition = "TEXT")
    private String imagenUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Moneda del precio. Por defecto ARS (default a nivel BD para filas existentes). */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "moneda", columnDefinition = "varchar(3) default 'ARS' not null")
    private Moneda moneda = Moneda.ARS;

    @Column(nullable = false)
    private Integer stock;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Category categoria;
}
