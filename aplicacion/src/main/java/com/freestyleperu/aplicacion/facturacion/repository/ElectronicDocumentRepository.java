package com.freestyleperu.aplicacion.facturacion.repository;

import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocument;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocumentType;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ElectronicDocumentRepository extends JpaRepository<ElectronicDocument, Long> {

    @Override
    @EntityGraph(attributePaths = { "sale" })
    Optional<ElectronicDocument> findById(Long id);

    @EntityGraph(attributePaths = { "sale" })
    List<ElectronicDocument> findAllBySaleIdOrderByCreatedAtDesc(Long saleId);

    @EntityGraph(attributePaths = { "sale" })
    Optional<ElectronicDocument> findBySaleIdAndDocumentType(Long saleId, ElectronicDocumentType documentType);

    @EntityGraph(attributePaths = { "sale" })
    Optional<ElectronicDocument> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            select d.provider, d.status, count(d), coalesce(sum(d.amount), 0)
            from ElectronicDocument d
            where d.createdAt >= :from and d.createdAt < :to
            group by d.provider, d.status
            order by d.provider, d.status
            """)
    List<Object[]> resumenPorEstado(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
