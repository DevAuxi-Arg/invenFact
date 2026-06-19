package com.willysoft.productosapi.cliente.dto;

import com.willysoft.productosapi.cliente.CondicionIva;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String nombre,

        @Size(max = 50, message = "El documento no puede exceder 50 caracteres")
        String documento,

        @Email(message = "El email no es válido")
        @Size(max = 150, message = "El email no puede exceder 150 caracteres")
        String email,

        @Size(max = 50, message = "El teléfono no puede exceder 50 caracteres")
        String telefono,

        @Size(max = 200, message = "La dirección no puede exceder 200 caracteres")
        String direccion,

        CondicionIva condicionIva
) {
}
