package com.willysoft.productosapi.cliente.dto;

import com.willysoft.productosapi.cliente.CondicionIva;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String documento,
        String email,
        String telefono,
        String direccion,
        CondicionIva condicionIva,
        boolean activo,
        LocalDateTime fechaCreacion
) {
}
