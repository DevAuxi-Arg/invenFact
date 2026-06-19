package com.willysoft.productosapi.web;

import com.willysoft.productosapi.auth.PasswordResetService;
import com.willysoft.productosapi.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@RequestParam String email, Model model) {
        passwordResetService.requestReset(email);
        // Mensaje neutro: no revela si el email existe.
        model.addAttribute("enviado", true);
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String token,
                        @RequestParam String passwordNueva,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.reset(token, passwordNueva);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Contraseña actualizada. Ya podés iniciar sesión.");
            return "redirect:/login";
        } catch (ForbiddenException e) {
            model.addAttribute("token", token);
            model.addAttribute("error", e.getMessage());
            return "reset-password";
        }
    }
}
