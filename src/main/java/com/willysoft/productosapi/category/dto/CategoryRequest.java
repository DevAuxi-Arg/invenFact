package com.willysoft.productosapi.category.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CategoryRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String descripcion,

        @DecimalMin(value = "0.00", message = "La alícuota de IVA no puede ser negativa")
        @DecimalMax(value = "100.00", message = "La alícuota de IVA no puede superar 100")
        @Digits(integer = 3, fraction = 2, message = "La alícuota debe tener máximo 3 enteros y 2 decimales")
        BigDecimal alicuotaIva,

        String iconoUrl,

        @PositiveOrZero(message = "El stock mínimo no puede ser negativo")
        Integer stockMinimo
) {
    /** Compatibilidad: alta/edición sin stock mínimo de categoría (se asume 0). */
    public CategoryRequest(String nombre, String descripcion, BigDecimal alicuotaIva, String iconoUrl) {
        this(nombre, descripcion, alicuotaIva, iconoUrl, 0);
    }
}
