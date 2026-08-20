package com.freestyleperu.aplicacion.caja.repository;

import com.freestyleperu.aplicacion.caja.domain.CashMovement;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    @EntityGraph(attributePaths = "user")
    Page<CashMovement> findAllByCashSessionIdOrderByCreatedAtDesc(Long cashSessionId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.amount), 0) FROM CashMovement m WHERE m.cashSession.id = :cashSessionId")
    BigDecimal sumaPorSesion(@Param("cashSessionId") Long cashSessionId);
}
