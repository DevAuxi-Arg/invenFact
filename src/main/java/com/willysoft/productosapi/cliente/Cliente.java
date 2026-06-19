package com.willysoft.productosapi.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    /** DNI / CUIT. Único cuando está presente (puede ser null). */
    @Column(unique = true, length = 50)
    private String documento;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25, columnDefinition = "varchar(25) default 'CONSUMIDOR_FINAL' not null")
    private CondicionIva condicionIva = CondicionIva.CONSUMIDOR_FINAL;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Column(updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(length = 150)
    private String creadoPor;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
