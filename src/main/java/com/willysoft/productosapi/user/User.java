package com.willysoft.productosapi.user;

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
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role rol;

    /** URL de la imagen/avatar del usuario (opcional). */
    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    /** Email alternativo para recibir el enlace de recuperación de contraseña (opcional).
        Si está vacío, se usa el email de login. */
    @Column(length = 150)
    private String emailRecuperacion;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean bloqueado = false;

    @Builder.Default
    @Column(nullable = false)
    private int intentosFallidos = 0;

    private LocalDateTime ultimoAcceso;

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
