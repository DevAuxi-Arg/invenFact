package com.willysoft.productosapi.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.willysoft.productosapi.exception.ForbiddenException;
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
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService service;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 604_800_000L);
    }

    @Test
    void rotate_revocaElActualYCreaUnoNuevo() {
        RefreshToken actual = RefreshToken.builder()
                .token("viejo").email("u@x.com")
                .expiryDate(LocalDateTime.now().plusDays(1)).revoked(false).build();
        when(repository.findByToken("viejo")).thenReturn(Optional.of(actual));
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken nuevo = service.rotate("viejo");

        assertThat(actual.isRevoked()).isTrue();
        assertThat(nuevo.getEmail()).isEqualTo("u@x.com");
        assertThat(nuevo.getToken()).isNotEqualTo("viejo");
        // se guarda el revocado y el nuevo
        verify(repository, org.mockito.Mockito.times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotate_fallaSiTokenRevocado() {
        RefreshToken revocado = RefreshToken.builder()
                .token("t").email("u@x.com")
                .expiryDate(LocalDateTime.now().plusDays(1)).revoked(true).build();
        when(repository.findByToken("t")).thenReturn(Optional.of(revocado));

        assertThatThrownBy(() -> service.rotate("t")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rotate_fallaSiTokenInexistente() {
        when(repository.findByToken("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rotate("nope")).isInstanceOf(ForbiddenException.class);
    }
}
