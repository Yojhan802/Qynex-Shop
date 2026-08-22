package com.freestyleperu.aplicacion.promocion.repository;

import com.freestyleperu.aplicacion.promocion.domain.Promocion;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    boolean existsByCode(String code);

    List<Promocion> findAllByOrderByNameAsc();

    /** Usado por el POS para ofrecer solo promociones vigentes y aplicables a una variante concreta. */
    @Query("""
            SELECT p FROM Promocion p
            WHERE p.status = 'ACTIVE'
              AND (p.startsAt IS NULL OR p.startsAt <= :ahora)
              AND (p.endsAt IS NULL OR p.endsAt >= :ahora)
              AND (p.scopeType = 'ALL'
                   OR (p.scopeType = 'CATEGORY' AND p.scopeCategory.id = :categoryId)
                   OR (p.scopeType = 'PRODUCT' AND p.scopeProduct.id = :productId))
            ORDER BY p.name
            """)
    List<Promocion> buscarVigentesParaProducto(
            @Param("productId") Long productId, @Param("categoryId") Long categoryId, @Param("ahora") LocalDateTime ahora);
}
