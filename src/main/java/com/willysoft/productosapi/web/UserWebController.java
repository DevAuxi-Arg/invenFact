package com.willysoft.productosapi.web;

import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ForbiddenException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.security.SecurityUtils;
import com.willysoft.productosapi.user.Role;
import com.willysoft.productosapi.user.UserService;
import com.willysoft.productosapi.user.dto.UpdateRoleRequest;
import com.willysoft.productosapi.user.dto.UserCreateRequest;
import com.willysoft.productosapi.user.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class UserWebController {

    private final UserService userService;

    /** Roles que el usuario actual puede asignar (ADMIN: todos; CO-ADMIN: solo BACKOFFICE). */
    private List<Role> rolesAsignables() {
        return SecurityUtils.isAdmin()
                ? List.of(Role.values())
                : List.of(Role.BACKOFFICE);
    }

    @GetMapping
    public String list(@RequestParam(required = false) String texto,
                       @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable,
                       Model model) {
        var page = userService.search(texto, pageable);
        model.addAttribute("page", page);
        model.addAttribute("usuarios", page.getContent());
        model.addAttribute("texto", texto);
        model.addAttribute("roles", List.of(Role.values()));
        return "admin/usuarios/list";
    }

    @GetMapping("/nuevo")
    public String createForm(Model model) {
        model.addAttribute("usuario", new UserCreateRequest("", "", "", Role.BACKOFFICE));
        model.addAttribute("roles", rolesAsignables());
        model.addAttribute("modo", "crear");
        return "admin/usuarios/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("usuario") UserCreateRequest usuario,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", rolesAsignables());
            model.addAttribute("modo", "crear");
            return "admin/usuarios/form";
        }
        try {
            userService.create(usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario creado correctamente");
            return "redirect:/admin/usuarios";
        } catch (ConflictException e) {
            bindingResult.rejectValue("email", "conflict", e.getMessage());
            model.addAttribute("roles", rolesAsignables());
            model.addAttribute("modo", "crear");
            return "admin/usuarios/form";
        } catch (ForbiddenException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @GetMapping("/{id}/editar")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            var u = userService.findById(id);
            model.addAttribute("usuario",
                    new UserUpdateRequest(u.nombre(), u.email(), u.activo(), u.avatarUrl(), u.emailRecuperacion()));
            model.addAttribute("id", id);
            model.addAttribute("rolActual", u.rol());
            model.addAttribute("roles", List.of(Role.values()));
            model.addAttribute("modo", "editar");
            return "admin/usuarios/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("usuario") UserUpdateRequest usuario,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("roles", List.of(Role.values()));
            model.addAttribute("modo", "editar");
            return "admin/usuarios/form";
        }
        try {
            userService.update(id, usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario actualizado correctamente");
        } catch (ConflictException | ForbiddenException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeRole(@PathVariable Long id,
                             @RequestParam Role rol,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.updateRole(id, new UpdateRoleRequest(rol));
            redirectAttributes.addFlashAttribute("mensaje", "Rol actualizado correctamente");
        } catch (ForbiddenException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado correctamente");
        } catch (ForbiddenException | ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }
}
