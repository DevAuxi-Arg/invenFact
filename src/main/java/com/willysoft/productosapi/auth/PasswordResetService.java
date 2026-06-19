package com.willysoft.productosapi.auth;

import com.willysoft.productosapi.exception.ForbiddenException;
import com.willysoft.productosapi.user.UserRepository;
import com.willysoft.productosapi.user.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${app.password-reset.expiration-minutes}")
    private long expirationMinutes;

    @Value("${app.password-reset.base-url}")
    private String baseUrl;

    /**
     * Genera un token de recuperación y envía el correo. Es deliberadamente silencioso
     * cuando el email no existe, para no revelar qué cuentas están registradas.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString().replace("-", "")
                            + UUID.randomUUID().toString().replace("-", ""))
                    .email(user.getEmail())
                    .expiryDate(LocalDateTime.now().plusMinutes(expirationMinutes))
                    .used(false)
                    .build();
            tokenRepository.save(token);

            String link = baseUrl + "?token=" + token.getToken();
            String body = """
                    Hola %s,

                    Recibimos una solicitud para restablecer tu contraseña.
                    Usá el siguiente enlace (válido por %d minutos):

                    %s

                    Si no solicitaste este cambio, podés ignorar este correo.

                    — Productos API · WillySoft""".formatted(user.getNombre(), expirationMinutes, link);

            emailService.send(user.getEmail(), "Restablecer tu contraseña", body);
        });
    }

    /** Valida el token y aplica la nueva contraseña. */
    @Transactional
    public void reset(String tokenValue, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ForbiddenException("Token de recuperación inválido"));
        if (!token.isUsable()) {
            throw new ForbiddenException("Token de recuperación expirado o ya utilizado");
        }
        userService.setPassword(token.getEmail(), newPassword);
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
