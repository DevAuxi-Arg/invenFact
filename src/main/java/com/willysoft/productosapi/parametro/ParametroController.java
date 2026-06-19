package com.willysoft.productosapi.parametro;

import com.willysoft.productosapi.parametro.dto.ParametroResponse;
import com.willysoft.productosapi.parametro.dto.ParametroUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parametros")
@RequiredArgsConstructor
@Tag(name = "Parámetros", description = "Parámetros globales del sistema (ej. cotización del dólar)")
@SecurityRequirement(name = "bearer-jwt")
public class ParametroController {

    private final ParametroService parametroService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Listar parámetros")
    public ResponseEntity<List<ParametroResponse>> findAll() {
        return ResponseEntity.ok(parametroService.findAll());
    }

    @GetMapping("/{clave}")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Obtener un parámetro por clave (ej. DOLAR)")
    public ResponseEntity<ParametroResponse> findByClave(@PathVariable String clave) {
        return ResponseEntity.ok(parametroService.findByClave(clave));
    }

    @PutMapping("/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar el valor de un parámetro (solo ADMIN)")
    public ResponseEntity<ParametroResponse> update(@PathVariable String clave,
                                                    @Valid @RequestBody ParametroUpdateRequest request) {
        return ResponseEntity.ok(parametroService.update(clave, request.valor()));
    }
}
