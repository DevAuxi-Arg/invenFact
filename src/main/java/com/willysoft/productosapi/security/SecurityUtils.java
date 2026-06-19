package com.willysoft.productosapi.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso al usuario autenticado en el contexto de seguridad.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Email (username) del usuario autenticado, o "sistema" si no hay sesión. */
    public static String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "sistema";
        }
        return auth.getName();
    }

    /** {@code true} si el usuario autenticado tiene el rol indicado (sin prefijo ROLE_). */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public static boolean isCoadmin() {
        return hasRole("COADMIN");
    }
}
