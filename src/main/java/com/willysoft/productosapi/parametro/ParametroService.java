package com.willysoft.productosapi.parametro;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.parametro.dto.ParametroResponse;
import com.willysoft.productosapi.security.SecurityUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParametroService {

    /** Clave del parámetro con la cotización del dólar (ARS por USD). */
    public static final String DOLAR = "DOLAR";

    /** Clave del parámetro con el stock mínimo global (umbral de alerta; 0 = sin aviso). */
    public static final String STOCK_MINIMO = "STOCK_MINIMO";

    private final ParametroRepository parametroRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ParametroResponse> findAll() {
        return parametroRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ParametroResponse findByClave(String clave) {
        return toResponse(getOrThrow(clave));
    }

    /** Cotización vigente del dólar (ARS por USD). */
    @Transactional(readOnly = true)
    public BigDecimal getDolar() {
        return getDecimal(DOLAR);
    }

    /** Stock mínimo global (0 = sin aviso). Devuelve 0 si no está definido o es inválido. */
    @Transactional(readOnly = true)
    public int getStockMinimoGlobal() {
        return parametroRepository.findById(STOCK_MINIMO)
                .map(p -> {
                    try {
                        return Math.max(0, Integer.parseInt(p.getValor().trim()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String clave) {
        try {
            return new BigDecimal(getOrThrow(clave).getValor());
        } catch (NumberFormatException e) {
            throw new ConflictException("El parámetro " + clave + " no es un número válido");
        }
    }

    @Transactional
    public ParametroResponse update(String clave, String valor) {
        Parametro parametro = getOrThrow(clave);
        if (DOLAR.equals(clave)) {
            validarDecimalPositivo(valor);
        } else if (STOCK_MINIMO.equals(clave)) {
            validarEnteroNoNegativo(valor);
        }
        parametro.setValor(valor);
        parametro.setFechaActualizacion(LocalDateTime.now());
        parametro.setActualizadoPor(SecurityUtils.currentEmail());
        Parametro saved = parametroRepository.save(parametro);
        auditService.log("PARAMETRO_ACTUALIZADO", clave + " = " + valor, SecurityUtils.currentEmail());
        return toResponse(saved);
    }

    private void validarDecimalPositivo(String valor) {
        BigDecimal bd;
        try {
            bd = new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw new ConflictException("El valor debe ser un número (ej. 1000.00)");
        }
        if (bd.signum() <= 0) {
            throw new ConflictException("El valor debe ser mayor que cero");
        }
    }

    private void validarEnteroNoNegativo(String valor) {
        int n;
        try {
            n = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new ConflictException("El valor debe ser un número entero (ej. 5)");
        }
        if (n < 0) {
            throw new ConflictException("El valor no puede ser negativo");
        }
    }

    private Parametro getOrThrow(String clave) {
        return parametroRepository.findById(clave)
                .orElseThrow(() -> new ResourceNotFoundException("Parámetro no encontrado: " + clave));
    }

    private ParametroResponse toResponse(Parametro p) {
        return new ParametroResponse(p.getClave(), p.getValor(), p.getDescripcion(),
                p.getFechaActualizacion(), p.getActualizadoPor());
    }
}
