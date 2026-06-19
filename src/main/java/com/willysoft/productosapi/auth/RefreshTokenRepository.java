package com.willysoft.productosapi.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.email = :email and r.revoked = false")
    int revokeAllByEmail(@Param("email") String email);

    @Modifying
    @Query("delete from RefreshToken r where r.expiryDate < :fecha")
    int deleteByExpiryDateBefore(@Param("fecha") LocalDateTime fecha);
}
