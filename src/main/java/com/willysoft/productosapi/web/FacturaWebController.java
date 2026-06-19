package com.willysoft.productosapi.web;

import com.willysoft.productosapi.cliente.ClienteService;
import com.willysoft.productosapi.cliente.CondicionIva;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.factura.FacturaService;
import com.willysoft.productosapi.factura.dto.FacturaCreateRequest;
import com.willysoft.productosapi.factura.dto.LineaFacturaRequest;
import com.willysoft.productosapi.product.ProductService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaWebController {

    private final FacturaService facturaService;
    private final ProductService productService;
    private final ClienteService clienteService;

    @org.springframework.web.bind.annotation.ModelAttribute("condiciones")
    public CondicionIva[] condiciones() {
        return CondicionIva.values();
    }

    @GetMapping
    public String list(@PageableDefault(size = 10, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model) {
        var page = facturaService.search(pageable);
        var orden = pageable.getSort().stream().findFirst().orElse(Sort.Order.desc("fecha"));
        model.addAttribute("page", page);
        model.addAttribute("facturas", page.getContent());
        model.addAttribute("sortField", orden.getProperty());
        model.addAttribute("sortDir", orden.getDirection().name().toLowerCase());
        return "facturas/list";
    }

    @GetMapping("/nueva")
    public String createForm(Model model) {
        model.addAttribute("productos", productService.search(null, null, Pageable.unpaged()).getContent());
        model.addAttribute("clientes", clienteService.findAll());
        return "facturas/form";
    }

    @PostMapping
    public String create(@RequestParam(required = false) Long clienteId,
                         @RequestParam(required = false) String clienteNombre,
                         @RequestParam(required = false) String clienteDocumento,
                         @RequestParam(required = false) CondicionIva condicionIva,
                         @RequestParam(defaultValue = "false") boolean registrarCliente,
                         @RequestParam(required = false) String observaciones,
                         @RequestParam(name = "productoId", required = false) List<Long> productoIds,
                         @RequestParam(name = "cantidad", required = false) List<Integer> cantidades,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        List<LineaFacturaRequest> lineas = new ArrayList<>();
        if (productoIds != null) {
            for (int i = 0; i < productoIds.size(); i++) {
                Long prodId = productoIds.get(i);
                Integer cant = (cantidades != null && i < cantidades.size()) ? cantidades.get(i) : null;
                if (prodId != null && cant != null && cant > 0) {
                    lineas.add(new LineaFacturaRequest(prodId, cant));
                }
            }
        }
        if (lineas.isEmpty()) {
            model.addAttribute("error", "Agregá al menos un producto con cantidad válida.");
            cargarOpciones(model);
            return "facturas/form";
        }
        try {
            var creada = facturaService.create(new FacturaCreateRequest(
                    clienteId, clienteNombre, clienteDocumento, condicionIva,
                    registrarCliente, observaciones, lineas));
            redirectAttributes.addFlashAttribute("mensaje", "Factura " + creada.numero() + " emitida correctamente");
            return "redirect:/facturas/" + creada.id();
        } catch (ConflictException | ResourceNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            cargarOpciones(model);
            return "facturas/form";
        }
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("productos", productService.search(null, null, Pageable.unpaged()).getContent());
        model.addAttribute("clientes", clienteService.findAll());
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("factura", facturaService.findById(id));
            return "facturas/detalle";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/facturas";
        }
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    public String anular(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            facturaService.anular(id);
            redirectAttributes.addFlashAttribute("mensaje", "Factura anulada y stock restituido");
        } catch (ConflictException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/facturas/" + id;
    }
}
