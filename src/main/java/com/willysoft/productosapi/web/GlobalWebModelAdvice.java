package com.willysoft.productosapi.web;

import com.willysoft.productosapi.security.SecurityUtils;
import com.willysoft.productosapi.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Expone el usuario autenticado (email, nombre, rol) y banderas de rol a todas las
 * plantillas Thymeleaf, para que el navbar y las vistas muestren/oculten opciones
 * sin depender de la integración Thymeleaf-Security.
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalWebModelAdvice {

    private final ObjectProvider<UserService> userServiceProvider;

    @ModelAttribute("currentEmail")
    public String currentEmail() {
        return emailOrNull();
    }

    @ModelAttribute("currentNombre")
    public String currentNombre() {
        String email = emailOrNull();
        if (email == null) {
            return null;
        }
        UserService userService = userServiceProvider.getIfAvailable();
        if (userService == null) {
            return email;
        }
        try {
            return userService.findByEmail(email).nombre();
        } catch (RuntimeException e) {
            return email; // fallback si el usuario ya no existe
        }
    }

    @ModelAttribute("currentAvatar")
    public String currentAvatar() {
        String email = emailOrNull();
        if (email == null) {
            return null;
        }
        UserService userService = userServiceProvider.getIfAvailable();
        if (userService == null) {
            return null;
        }
        try {
            return userService.findByEmail(email).avatarUrl();
        } catch (RuntimeException e) {
            return null; // fallback si el usuario ya no existe
        }
    }

    @ModelAttribute("currentRol")
    public String currentRol() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }

    @ModelAttribute("isCoadmin")
    public boolean isCoadmin() {
        return SecurityUtils.isCoadmin();
    }

    private String emailOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }
}
