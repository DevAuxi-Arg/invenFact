package com.willysoft.productosapi.web;

import com.willysoft.productosapi.category.CategoryService;
import com.willysoft.productosapi.category.dto.CategoryRequest;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoryWebController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(@RequestParam(required = false) String nombre,
                       @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model) {
        var page = categoryService.search(nombre, pageable);
        model.addAttribute("page", page);
        model.addAttribute("categorias", page.getContent());
        model.addAttribute("nombre", nombre);
        return "categorias/list";
    }

    @GetMapping("/nueva")
    public String createForm(Model model) {
        model.addAttribute("categoria", new CategoryRequest("", "", new BigDecimal("21.00"), ""));
        model.addAttribute("modo", "crear");
        return "categorias/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoria") CategoryRequest categoria,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modo", "crear");
            return "categorias/form";
        }
        try {
            categoryService.create(categoria);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría creada correctamente");
            return "redirect:/categorias";
        } catch (ConflictException e) {
            bindingResult.rejectValue("nombre", "conflict", e.getMessage());
            model.addAttribute("modo", "crear");
            return "categorias/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            var c = categoryService.findById(id);
            model.addAttribute("categoria",
                    new CategoryRequest(c.nombre(), c.descripcion(), c.alicuotaIva(), c.iconoUrl(), c.stockMinimo()));
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "categorias/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoria") CategoryRequest categoria,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "categorias/form";
        }
        try {
            categoryService.update(id, categoria);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría actualizada correctamente");
            return "redirect:/categorias";
        } catch (ConflictException e) {
            bindingResult.rejectValue("nombre", "conflict", e.getMessage());
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "categorias/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias";
        }
    }

    @PostMapping("/{id}/eliminar")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.delete(id);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría eliminada correctamente");
        } catch (ConflictException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/categorias";
    }
}
