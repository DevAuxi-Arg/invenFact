package com.willysoft.productosapi.web;

import com.willysoft.productosapi.cliente.ClienteService;
import com.willysoft.productosapi.cliente.CondicionIva;
import com.willysoft.productosapi.cliente.dto.ClienteRequest;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteWebController {

    private final ClienteService clienteService;

    @ModelAttribute("condiciones")
    public CondicionIva[] condiciones() {
        return CondicionIva.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) String texto,
                       @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model) {
        var page = clienteService.search(texto, pageable);
        model.addAttribute("page", page);
        model.addAttribute("clientes", page.getContent());
        model.addAttribute("texto", texto);
        return "clientes/list";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        model.addAttribute("cliente",
                new ClienteRequest("", "", "", "", "", CondicionIva.CONSUMIDOR_FINAL));
        model.addAttribute("modo", "crear");
        return "clientes/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("cliente") ClienteRequest cliente,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modo", "crear");
            return "clientes/form";
        }
        try {
            clienteService.create(cliente);
            redirectAttributes.addFlashAttribute("mensaje", "Cliente creado correctamente");
            return "redirect:/clientes";
        } catch (ConflictException e) {
            bindingResult.rejectValue("documento", "conflict", e.getMessage());
            model.addAttribute("modo", "crear");
            return "clientes/form";
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            var c = clienteService.findById(id);
            model.addAttribute("cliente", new ClienteRequest(c.nombre(), c.documento(), c.email(),
                    c.telefono(), c.direccion(), c.condicionIva()));
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "clientes/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/clientes";
        }
    }

    @PostMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("cliente") ClienteRequest cliente,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("modo", "editar");
            return "clientes/form";
        }
        try {
            clienteService.update(id, cliente);
            redirectAttributes.addFlashAttribute("mensaje", "Cliente actualizado correctamente");
        } catch (ConflictException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasAnyRole('ADMIN','COADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clienteService.delete(id);
            redirectAttributes.addFlashAttribute("mensaje", "Cliente eliminado correctamente");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clientes";
    }
}
