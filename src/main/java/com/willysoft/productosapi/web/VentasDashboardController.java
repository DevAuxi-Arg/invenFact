package com.willysoft.productosapi.web;

import com.willysoft.productosapi.factura.VentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/ventas")
@RequiredArgsConstructor
public class VentasDashboardController {

    private final VentasService ventasService;

    @GetMapping
    public String dashboard(Model model) {
        var dashboard = ventasService.dashboard();
        model.addAttribute("resumen", dashboard.resumen());
        model.addAttribute("mesesLabels", dashboard.mesesLabels());
        model.addAttribute("mesesData", dashboard.mesesData());
        model.addAttribute("topProductos", dashboard.topProductos());
        model.addAttribute("topClientes", dashboard.topClientes());
        return "admin/ventas";
    }
}
