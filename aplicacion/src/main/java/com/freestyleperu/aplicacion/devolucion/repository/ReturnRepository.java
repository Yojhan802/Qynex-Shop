package com.freestyleperu.aplicacion.devolucion.repository;

import com.freestyleperu.aplicacion.devolucion.domain.Return;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnRepository extends JpaRepository<Return, Long> {

    @EntityGraph(attributePaths = { "sale", "user", "refundMethod" })
    @Query("SELECT r FROM Return r WHERE (:saleId IS NULL OR r.sale.id = :saleId) ORDER BY r.createdAt DESC")
    Page<Return> buscar(@Param("saleId") Long saleId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "sale", "user", "refundMethod" })
    Optional<Return> findById(Long id);
}
