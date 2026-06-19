package com.willysoft.productosapi.user.dto;

import com.willysoft.productosapi.user.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String nombre,
        String email,
        Role rol,
        String avatarUrl,
        boolean activo,
        boolean bloqueado,
        LocalDateTime ultimoAcceso,
        LocalDateTime fechaCreacion
) {
}
