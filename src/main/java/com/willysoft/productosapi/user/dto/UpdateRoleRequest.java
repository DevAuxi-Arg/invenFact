package com.willysoft.productosapi.user.dto;

import com.willysoft.productosapi.user.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "El rol es obligatorio")
        Role rol
) {
}
