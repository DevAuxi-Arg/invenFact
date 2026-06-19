package com.willysoft.productosapi.factura;

import com.willysoft.productosapi.factura.dto.FacturaCreateRequest;
import com.willysoft.productosapi.factura.dto.FacturaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "Emisión de comprobantes con valores congelados (snapshot)")
@SecurityRequirement(name = "bearer-jwt")
public class FacturaController {

    private final FacturaService facturaService;

    @GetMapping
    @Operation(summary = "Listar facturas paginadas (más recientes primero)")
    public ResponseEntity<Page<FacturaResponse>> search(
            @PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(facturaService.search(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una factura por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Factura encontrada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada")
    })
    public ResponseEntity<FacturaResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN','BACKOFFICE')")
    @Operation(summary = "Emitir una factura",
            description = "Congela precio, tipo de cambio e IVA de cada línea y descuenta stock.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Factura emitida"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Algún producto no existe"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    })
    public ResponseEntity<FacturaResponse> create(@Valid @RequestBody FacturaCreateRequest request) {
        FacturaResponse created = facturaService.create(request);
        return ResponseEntity.created(URI.create("/api/facturas/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Anular una factura (devuelve el stock)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Factura anulada"),
            @ApiResponse(responseCode = "404", description = "Factura no encontrada"),
            @ApiResponse(responseCode = "409", description = "La factura ya estaba anulada")
    })
    public ResponseEntity<FacturaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(facturaService.anular(id));
    }
}
