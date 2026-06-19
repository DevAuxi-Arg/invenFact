package com.willysoft.productosapi.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.exception.ForbiddenException;
import com.willysoft.productosapi.user.User;
import com.willysoft.productosapi.user.UserRepository;
import com.willysoft.productosapi.user.UserService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordResetService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "expirationMinutes", 30L);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080/reset-password");
    }

    @Test
    void requestReset_creaTokenYEnviaEmail_siUsuarioExiste() {
        User user = User.builder().nombre("Ana").email("ana@x.com").build();
        when(userRepository.findByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(user));

        service.requestReset("ana@x.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).send(eq("ana@x.com"), anyString(), anyString());
    }

    @Test
    void requestReset_silencioso_siUsuarioNoExiste() {
        when(userRepository.findByEmailIgnoreCase("nadie@x.com")).thenReturn(Optional.empty());

        service.requestReset("nadie@x.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void reset_aplicaPassword_siTokenValido() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("ok").email("ana@x.com")
                .expiryDate(LocalDateTime.now().plusMinutes(10)).used(false).build();
        when(tokenRepository.findByToken("ok")).thenReturn(Optional.of(token));

        service.reset("ok", "nuevaClave123");

        verify(userService).setPassword("ana@x.com", "nuevaClave123");
        verify(tokenRepository).save(token);
    }

    @Test
    void reset_fallaSiTokenExpirado() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("old").email("ana@x.com")
                .expiryDate(LocalDateTime.now().minusMinutes(1)).used(false).build();
        when(tokenRepository.findByToken("old")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.reset("old", "x12345678"))
                .isInstanceOf(ForbiddenException.class);
        verify(userService, never()).setPassword(anyString(), anyString());
    }
}
