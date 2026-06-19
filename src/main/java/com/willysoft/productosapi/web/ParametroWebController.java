package com.willysoft.productosapi.web;

import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.parametro.ParametroService;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/admin/parametros")
@RequiredArgsConstructor
public class ParametroWebController {

    private final ParametroService parametroService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("parametros", parametroService.findAll());
        return "admin/parametros/list";
    }

    @PostMapping("/{clave}")
    @PreAuthorize("hasRole('ADMIN')")
    public String update(@PathVariable String clave,
                         @RequestParam String valor,
                         RedirectAttributes redirectAttributes) {
        try {
            parametroService.update(clave, valor);
            redirectAttributes.addFlashAttribute("mensaje", "Parámetro actualizado correctamente");
        } catch (ConflictException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/parametros";
    }
}
