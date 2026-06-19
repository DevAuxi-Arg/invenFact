package com.willysoft.productosapi.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.audit.AuditService;
import com.willysoft.productosapi.auth.RefreshTokenService;
import com.willysoft.productosapi.exception.ForbiddenException;
import com.willysoft.productosapi.user.dto.UpdateRoleRequest;
import com.willysoft.productosapi.user.dto.UserCreateRequest;
import com.willysoft.productosapi.user.dto.UserUpdateRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, "x", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void coadmin_puedeCrearBackoffice() {
        authenticateAs("co@willysoft.com", "COADMIN");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new UserCreateRequest("Op", "op@willysoft.com", "12345678", Role.BACKOFFICE);
        var resp = userService.create(req);

        assertThat(resp.rol()).isEqualTo(Role.BACKOFFICE);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void coadmin_noPuedeCrearCoadmin() {
        authenticateAs("co@willysoft.com", "COADMIN");

        var req = new UserCreateRequest("Otro", "otro@willysoft.com", "12345678", Role.COADMIN);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(ForbiddenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void admin_puedeCrearCoadmin() {
        authenticateAs("admin@willysoft.com", "ADMIN");
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        var req = new UserCreateRequest("Co", "co2@willysoft.com", "12345678", Role.COADMIN);
        var resp = userService.create(req);

        assertThat(resp.rol()).isEqualTo(Role.COADMIN);
    }

    @Test
    void coadmin_noPuedeEditarAdmin() {
        authenticateAs("co@willysoft.com", "COADMIN");
        User target = User.builder().id(1L).nombre("Admin").email("admin@willysoft.com")
                .password("h").rol(Role.ADMIN).activo(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));

        var req = new UserUpdateRequest("Admin", "admin@willysoft.com", true);

        assertThatThrownBy(() -> userService.update(1L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void noPuedeCambiarSuPropioRol() {
        authenticateAs("admin@willysoft.com", "ADMIN");
        User self = User.builder().id(1L).nombre("Admin").email("admin@willysoft.com")
                .password("h").rol(Role.ADMIN).activo(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> userService.updateRole(1L, new UpdateRoleRequest(Role.BACKOFFICE)))
                .isInstanceOf(ForbiddenException.class);
    }
}
