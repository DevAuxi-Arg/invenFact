package com.willysoft.productosapi.auth;

import com.willysoft.productosapi.exception.ForbiddenException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /** Crea y persiste un refresh token opaco para el usuario. */
    @Transactional
    public RefreshToken create(String email) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""))
                .email(email)
                .expiryDate(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(token);
    }

    /**
     * Verifica un refresh token y lo rota: revoca el actual y emite uno nuevo.
     * Lanza {@link ForbiddenException} si el token no existe, está revocado o expiró.
     */
    @Transactional
    public RefreshToken rotate(String tokenValue) {
        RefreshToken current = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ForbiddenException("Refresh token inválido"));
        if (!current.isActive()) {
            throw new ForbiddenException("Refresh token revocado o expirado");
        }
        current.setRevoked(true);
        refreshTokenRepository.save(current);
        return create(current.getEmail());
    }

    /** Revoca un refresh token concreto (logout de una sesión). */
    @Transactional
    public void revoke(String tokenValue) {
        refreshTokenRepository.findByToken(tokenValue).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    /** Revoca todas las sesiones de un usuario (logout global / cambio de contraseña). */
    @Transactional
    public void revokeAllForUser(String email) {
        refreshTokenRepository.revokeAllByEmail(email);
    }

    @Transactional
    public int deleteExpired() {
        return refreshTokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
