package com.willysoft.productosapi.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    /** URL del ícono de la categoría (PNG o GIF, opcional). */
    @Column(name = "icono_url", columnDefinition = "TEXT")
    private String iconoUrl;

    /** Alícuota de IVA en porcentaje (ej. 21.00, 10.50, 0.00 = exento). */
    @Builder.Default
    @Column(name = "alicuota_iva", columnDefinition = "numeric(5,2) default 21.00 not null")
    private BigDecimal alicuotaIva = new BigDecimal("21.00");
}
