package com.willysoft.productosapi.factura.dto;

import com.willysoft.productosapi.cliente.CondicionIva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Para el cliente hay dos caminos: indicar {@code clienteId} (cliente existente),
 * o cargar uno nuevo con {@code clienteNombre}/{@code clienteDocumento}. Si además
 * {@code registrarCliente} es true, ese cliente nuevo se guarda en la base.
 */
public record FacturaCreateRequest(
        Long clienteId,

        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String clienteNombre,

        @Size(max = 50, message = "El documento no puede exceder 50 caracteres")
        String clienteDocumento,

        CondicionIva condicionIva,

        boolean registrarCliente,

        @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
        String observaciones,

        @NotEmpty(message = "La factura debe tener al menos una línea")
        @Valid
        List<LineaFacturaRequest> lineas
) {
}
