package com.willysoft.productosapi.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Actualización de datos del usuario. El rol y la contraseña se cambian por
 * endpoints dedicados (solo ADMIN para el rol).
 */
public record UserUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 150, message = "El email no puede exceder 150 caracteres")
        String email,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo,

        @Size(max = 512, message = "La URL de la imagen no puede exceder 512 caracteres")
        String avatarUrl,

        @Email(message = "El email de recuperación no es válido")
        @Size(max = 150, message = "El email de recuperación no puede exceder 150 caracteres")
        String emailRecuperacion
) {
    /** Compatibilidad: actualización sin tocar la imagen. */
    public UserUpdateRequest(String nombre, String email, Boolean activo) {
        this(nombre, email, activo, null, null);
    }
}
