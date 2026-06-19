package com.willysoft.productosapi.factura;

import com.willysoft.productosapi.factura.dto.VentasDashboard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Métricas y rankings de ventas (facturas emitidas)")
@SecurityRequirement(name = "bearer-jwt")
public class VentasController {

    private final VentasService ventasService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    @Operation(summary = "Dashboard de ventas: KPIs, ventas por mes y rankings de productos y clientes")
    public ResponseEntity<VentasDashboard> dashboard() {
        return ResponseEntity.ok(ventasService.dashboard());
    }
}
