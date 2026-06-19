package com.willysoft.productosapi.security;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Audita y contabiliza los intentos de login de la parte web (sesión).
 */
@Component
@RequiredArgsConstructor
public class LoginAuditListener {

    private final UserService userService;
    private final AuditService auditService;

    @Value("${app.jwt.max-failed-attempts}")
    private int maxFailedAttempts;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        userService.registerLoginResult(email, true, maxFailedAttempts);
        auditService.log("LOGIN", "Login web correcto", email);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String email = principal != null ? principal.toString() : "desconocido";
        userService.registerLoginResult(email, false, maxFailedAttempts);
        auditService.log("LOGIN_FALLIDO", "Intento fallido de " + email, email);
    }
}
