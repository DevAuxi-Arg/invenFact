package com.willysoft.productosapi.user.dto;

import com.willysoft.productosapi.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 150, message = "El email no puede exceder 150 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password,

        @NotNull(message = "El rol es obligatorio")
        Role rol,

        @Size(max = 512, message = "La URL de la imagen no puede exceder 512 caracteres")
        String avatarUrl
) {
    /** Compatibilidad: alta sin imagen. */
    public UserCreateRequest(String nombre, String email, String password, Role rol) {
        this(nombre, email, password, rol, null);
    }
}
