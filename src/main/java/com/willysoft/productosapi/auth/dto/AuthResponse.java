package com.willysoft.productosapi.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String email,
        String rol
) {
}
