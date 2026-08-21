package com.freestyleperu.aplicacion.cliente.repository;

import com.freestyleperu.aplicacion.cliente.domain.CustomerRefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRefreshTokenRepository extends JpaRepository<CustomerRefreshToken, Long> {

    Optional<CustomerRefreshToken> findByTokenHash(String tokenHash);
}
