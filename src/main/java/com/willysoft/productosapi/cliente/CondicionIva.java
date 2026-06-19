package com.willysoft.productosapi.cliente;

/**
 * Condición del cliente frente al IVA. Determina si la venta lleva IVA:
 * {@link #EXENTO} no se grava; el resto sí (según la alícuota de cada categoría).
 */
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO,
    MONOTRIBUTO,
    EXENTO,
    CONSUMIDOR_FINAL
}
