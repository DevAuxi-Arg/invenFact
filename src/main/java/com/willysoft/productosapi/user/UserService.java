package com.willysoft.productosapi.user;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.auth.RefreshTokenService;
import com.willysoft.productosapi.exception.ConflictException;
import com.willysoft.productosapi.exception.ForbiddenException;
import com.willysoft.productosapi.exception.ResourceNotFoundException;
import com.willysoft.productosapi.security.SecurityUtils;
import com.willysoft.productosapi.user.dto.ChangePasswordRequest;
import com.willysoft.productosapi.user.dto.UpdateRoleRequest;
import com.willysoft.productosapi.user.dto.UserCreateRequest;
import com.willysoft.productosapi.user.dto.UserResponse;
import com.willysoft.productosapi.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String texto, Pageable pageable) {
        Page<User> page = StringUtils.hasText(texto)
                ? userRepository.findByNombreContainingIgnoreCaseOrEmailContainingIgnoreCase(texto, texto, pageable)
                : userRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getUserOrThrow(id));
    }

    @Transactional(readOnly = true)
    public UserResponse findByEmail(String email) {
        return toResponse(userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email)));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        assertCanAssignRole(request.rol());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.email());
        }
        User user = User.builder()
                .nombre(request.nombre())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .avatarUrl(normalizeUrl(request.avatarUrl()))
                .emailRecuperacion(normalizeUrl(request.emailRecuperacion()))
                .activo(true)
                .bloqueado(false)
                .creadoPor(SecurityUtils.currentEmail())
                .build();
        User saved = userRepository.save(user);
        auditService.log("USUARIO_CREADO",
                "Creado " + saved.getEmail() + " (" + saved.getRol() + ")", SecurityUtils.currentEmail());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUserOrThrow(id);
        assertCanManage(user);
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Ya existe un usuario con el email: " + request.email());
        }
        user.setNombre(request.nombre());
        user.setEmail(request.email());
        user.setAvatarUrl(normalizeUrl(request.avatarUrl()));
        user.setEmailRecuperacion(normalizeUrl(request.emailRecuperacion()));
        user.setActivo(request.activo());
        User saved = userRepository.save(user);
        if (!request.activo()) {
            // Al desactivar, se cierran todas sus sesiones activas.
            refreshTokenService.revokeAllForUser(saved.getEmail());
        }
        auditService.log("USUARIO_ACTUALIZADO", "Actualizado " + saved.getEmail(), SecurityUtils.currentEmail());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateRole(Long id, UpdateRoleRequest request) {
        User user = getUserOrThrow(id);
        if (user.getEmail().equalsIgnoreCase(SecurityUtils.currentEmail())) {
            throw new ForbiddenException("No puedes cambiar tu propio rol");
        }
        Role anterior = user.getRol();
        user.setRol(request.rol());
        User saved = userRepository.save(user);
        auditService.log("ROL_CAMBIADO",
                saved.getEmail() + ": " + anterior + " -> " + request.rol(), SecurityUtils.currentEmail());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = getUserOrThrow(id);
        if (user.getEmail().equalsIgnoreCase(SecurityUtils.currentEmail())) {
            throw new ForbiddenException("No puedes eliminar tu propia cuenta");
        }
        refreshTokenService.revokeAllForUser(user.getEmail());
        userRepository.delete(user);
        auditService.log("USUARIO_ELIMINADO", "Eliminado " + user.getEmail(), SecurityUtils.currentEmail());
    }

    @Transactional
    public void changeOwnPassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        if (!passwordEncoder.matches(request.passwordActual(), user.getPassword())) {
            throw new ForbiddenException("La contraseña actual no es correcta");
        }
        user.setPassword(passwordEncoder.encode(request.passwordNueva()));
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(email);
        auditService.log("PASSWORD_CAMBIADA", "Cambio de contraseña propia", email);
    }

    /**
     * Establece una nueva contraseña sin requerir la anterior (usado por el flujo de
     * recuperación). Revoca todas las sesiones activas del usuario.
     */
    @Transactional
    public void setPassword(String email, String rawPassword) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setBloqueado(false);
        user.setIntentosFallidos(0);
        userRepository.save(user);
        refreshTokenService.revokeAllForUser(email);
        auditService.log("PASSWORD_RESET", "Contraseña restablecida", email);
    }

    /**
     * Registra el resultado de un intento de login para bloqueo por intentos fallidos.
     */
    @Transactional
    public void registerLoginResult(String email, boolean exitoso, int maxFailedAttempts) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (exitoso) {
                user.setIntentosFallidos(0);
                user.setUltimoAcceso(java.time.LocalDateTime.now());
            } else {
                int intentos = user.getIntentosFallidos() + 1;
                user.setIntentosFallidos(intentos);
                if (intentos >= maxFailedAttempts) {
                    user.setBloqueado(true);
                    auditService.log("USUARIO_BLOQUEADO",
                            "Bloqueado por " + intentos + " intentos fallidos", email);
                }
            }
            userRepository.save(user);
        });
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    /**
     * Regla central de jerarquía: un CO-ADMIN solo puede gestionar usuarios BACKOFFICE.
     * Un ADMIN puede gestionar cualquier rol.
     */
    private void assertCanManage(User target) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isCoadmin() && target.getRol() == Role.BACKOFFICE) {
            return;
        }
        throw new ForbiddenException("No tienes permisos para gestionar a este usuario");
    }

    private void assertCanAssignRole(Role role) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        if (SecurityUtils.isCoadmin() && role == Role.BACKOFFICE) {
            return;
        }
        throw new ForbiddenException("Un CO-ADMIN solo puede dar de alta usuarios BACKOFFICE");
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getNombre(),
                user.getEmail(),
                user.getRol(),
                user.getAvatarUrl(),
                user.getEmailRecuperacion(),
                user.isActivo(),
                user.isBloqueado(),
                user.getUltimoAcceso(),
                user.getFechaCreacion()
        );
    }

    /** Normaliza la URL del avatar: vacío o solo espacios se guarda como null. */
    private static String normalizeUrl(String url) {
        return StringUtils.hasText(url) ? url.trim() : null;
    }
}
