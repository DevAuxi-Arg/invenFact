package com.willysoft.productosapi.parametro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Parámetro global del sistema (clave/valor) mantenido por el ADMIN.
 * Ej.: {@code DOLAR = 1000.00}. Extensible a otros parámetros futuros.
 */
@Entity
@Table(name = "parametros")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parametro {

    @Id
    @Column(length = 40)
    private String clave;

    @Column(nullable = false, length = 200)
    private String valor;

    @Column(length = 200)
    private String descripcion;

    private LocalDateTime fechaActualizacion;

    @Column(length = 150)
    private String actualizadoPor;
}
