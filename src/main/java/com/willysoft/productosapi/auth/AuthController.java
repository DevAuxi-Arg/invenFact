package com.willysoft.productosapi.auth;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.auth.dto.AuthResponse;
import com.willysoft.productosapi.auth.dto.ForgotPasswordRequest;
import com.willysoft.productosapi.auth.dto.LoginRequest;
import com.willysoft.productosapi.auth.dto.RefreshRequest;
import com.willysoft.productosapi.auth.dto.ResetPasswordRequest;
import com.willysoft.productosapi.security.JwtService;
import com.willysoft.productosapi.user.UserService;
import com.willysoft.productosapi.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, refresh y logout con tokens JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final AuditService auditService;

    @Value("${app.jwt.max-failed-attempts}")
    private int maxFailedAttempts;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener access + refresh token",
            description = "Valida email y contraseña. Devuelve un access token JWT (corto) y un "
                    + "refresh token opaco (largo, persistido en BD). Tras 5 intentos fallidos la cuenta se bloquea.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación correcta"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciales inválidas o cuenta bloqueada/inactiva")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (AuthenticationException ex) {
            userService.registerLoginResult(request.email(), false, maxFailedAttempts);
            auditService.log("LOGIN_FALLIDO", "Intento fallido de " + request.email(), request.email());
            throw ex;
        }

        userService.registerLoginResult(request.email(), true, maxFailedAttempts);
        UserResponse user = userService.findByEmail(request.email());
        String access = jwtService.generateAccessToken(user.email(), user.rol().name());
        String refresh = refreshTokenService.create(user.email()).getToken();
        auditService.log("LOGIN", "Login API correcto", user.email());
        return ResponseEntity.ok(buildResponse(user.email(), user.rol().name(), access, refresh));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar el access token (rota el refresh token)",
            description = "Verifica el refresh token; si es válido lo revoca y emite un nuevo par "
                    + "access + refresh (rotación). Un refresh revocado o expirado devuelve 403.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nuevo par de tokens emitido"),
            @ApiResponse(responseCode = "403", description = "Refresh token inválido, revocado o expirado")
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken rotated = refreshTokenService.rotate(request.refreshToken());
        UserResponse user = userService.findByEmail(rotated.getEmail());
        String access = jwtService.generateAccessToken(user.email(), user.rol().name());
        return ResponseEntity.ok(buildResponse(user.email(), user.rol().name(), access, rotated.getToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión revocando el refresh token",
            description = "Revoca el refresh token indicado. El access token sigue siendo válido "
                    + "hasta que expire (es stateless), pero ya no podrá renovarse.")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada (idempotente)")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        auditService.log("LOGOUT", "Logout API", "sistema");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Solicitar recuperación de contraseña (envía email con un token)",
            description = "Genera un token de un solo uso y envía un email con el enlace de restablecimiento. "
                    + "Responde 204 exista o no el email, para no revelar qué cuentas están registradas.")
    @ApiResponse(responseCode = "204", description = "Solicitud procesada (respuesta neutra)")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        // Siempre 204, exista o no el email, para no revelar cuentas registradas.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Restablecer la contraseña usando el token recibido por email",
            description = "Valida el token de recuperación y aplica la nueva contraseña. "
                    + "Al hacerlo se revocan todas las sesiones activas del usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña restablecida"),
            @ApiResponse(responseCode = "400", description = "La nueva contraseña no cumple los requisitos"),
            @ApiResponse(responseCode = "403", description = "Token inválido, expirado o ya utilizado")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.passwordNueva());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse buildResponse(String email, String rol, String access, String refresh) {
        return new AuthResponse(access, refresh, "Bearer", jwtService.getAccessExpirationMs() / 1000, email, rol);
    }
}
