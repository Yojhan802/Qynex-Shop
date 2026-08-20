package com.freestyleperu.aplicacion.auth.repository;

import com.freestyleperu.aplicacion.auth.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now WHERE rt.usuario.id = :usuarioId AND rt.revokedAt IS NULL")
    void revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId, @Param("now") LocalDateTime now);
}
