package com.willysoft.productosapi.web;

import com.willysoft.productosapi.category.CategoryService;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.product.CatalogoExportService;
import com.willysoft.productosapi.product.Moneda;
import com.willysoft.productosapi.product.ProductService;
import com.willysoft.productosapi.product.dto.ProductRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CatalogoExportService catalogoExportService;

    @ModelAttribute("monedas")
    public Moneda[] monedas() {
        return Moneda.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String nombre,
                       @RequestParam(required = false) Long categoriaId,
                       @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model) {
        var page = productService.search(nombre, categoriaId, pageable);
        model.addAttribute("page", page);
        model.addAttribute("productos", page.getContent());
        model.addAttribute("categorias", categoryService.findAll());
        model.addAttribute("nombre", nombre);
        model.addAttribute("categoriaId", categoriaId);
        return "productos/list";
    }

    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(name = "categoriaIds", required = false) List<Long> categoriaIds,
                           Model model) {
        var grupos = productService.catalogoPorCategoria(categoriaIds);
        model.addAttribute("grupos", grupos);
        model.addAttribute("categorias", categoryService.findAll());
        model.addAttribute("categoriaIds", categoriaIds);
        model.addAttribute("totalProductos",
                grupos.stream().mapToInt(g -> g.productos().size()).sum());
        return "productos/catalogo";
    }

    @GetMapping("/catalogo/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(name = "categoriaIds", required = false) List<Long> categoriaIds) {
        byte[] data = catalogoExportService.toExcel(productService.catalogoPorCategoria(categoriaIds));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo("xlsx") + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/catalogo/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(name = "categoriaIds", required = false) List<Long> categoriaIds) {
        byte[] data = catalogoExportService.toPdf(productService.catalogoPorCategoria(categoriaIds));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo("pdf") + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    /** Nombre del archivo exportado, con fecha y hora: lista-precios_2026-06-19_16hs30min.pdf */
    private String nombreArchivo(String extension) {
        String ts = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'hs'mm'min'"));
        return "lista-precios_" + ts + "." + extension;
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        model.addAttribute("producto", new ProductRequest("", "", BigDecimal.ZERO, 0, null, Moneda.ARS, ""));
        model.addAttribute("categorias", categoryService.findAll());
        model.addAttribute("monedas", Moneda.values());
        model.addAttribute("modo", "crear");
        return "productos/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("producto") ProductRequest producto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categorias", categoryService.findAll());
            model.addAttribute("modo", "crear");
            return "productos/form";
        }
        try {
            productService.create(producto);
            redirectAttributes.addFlashAttribute("mensaje", "Producto creado correctamente");
            return "redirect:/productos";
        } catch (ResourceNotFoundException e) {
            bindingResult.rejectValue("categoriaId", "notfound", e.getMessage());
            model.addAttribute("categorias", categoryService.findAll());
            model.addAttribute("modo", "crear");
            return "productos/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            var p = productService.findById(id);
            model.addAttribute("producto",
                    new ProductRequest(p.nombre(), p.descripcion(), p.precio(), p.stock(),
                            p.categoria().id(), p.moneda(), p.imagenUrl()));
            model.addAttribute("categorias", categoryService.findAll());
            model.addAttribute("monedas", Moneda.values());
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "productos/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("producto") ProductRequest producto,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categorias", categoryService.findAll());
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "productos/form";
        }
        try {
            productService.update(id, producto);
            redirectAttributes.addFlashAttribute("mensaje", "Producto actualizado correctamente");
            return "redirect:/productos";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/productos";
        }
    }

    /** Ajuste de stock vía AJAX (sin recargar la página). Responde JSON. */
    @PostMapping("/{id}/stock")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ajustarStock(@PathVariable Long id,
                                                            @RequestParam String modo,
                                                            @RequestParam(defaultValue = "0") int valor) {
        try {
            var p = productService.ajustarStock(id, modo, valor);
            Map<String, Object> body = Map.of(
                    "ok", true,
                    "stock", p.stock(),
                    "nombre", p.nombre(),
                    "mensaje", "Stock de " + p.nombre() + " actualizado: " + p.stock() + " unidades.");
            return ResponseEntity.ok(body);
        } catch (ConflictException e) {
            Map<String, Object> body = Map.of("ok", false, "mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
        } catch (ResourceNotFoundException e) {
            Map<String, Object> body = Map.of("ok", false, "mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado correctamente");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/productos";
    }
}
