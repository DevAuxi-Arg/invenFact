package com.willysoft.productosapi.parametro.dto;

import java.time.LocalDateTime;

public record ParametroResponse(
        String clave,
        String valor,
        String descripcion,
        LocalDateTime fechaActualizacion,
        String actualizadoPor
) {
}
