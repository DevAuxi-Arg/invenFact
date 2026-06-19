package com.willysoft.productosapi.auth;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Borra periódicamente los refresh tokens expirados de la base de datos.
 */
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupJob.class);

    private final RefreshTokenService refreshTokenService;

    /** Cada hora (en milisegundos), con un retardo inicial de 5 minutos. */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
    public void purgeExpired() {
        int borrados = refreshTokenService.deleteExpired();
        if (borrados > 0) {
            log.info("Limpieza de refresh tokens: {} expirados eliminados", borrados);
        }
    }
}
