package com.freestyleperu.aplicacion.venta.repository;

import com.freestyleperu.aplicacion.venta.domain.SaleDetail;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleDetailRepository extends JpaRepository<SaleDetail, Long> {

    @EntityGraph(attributePaths = "variant")
    List<SaleDetail> findAllBySaleId(Long saleId);

    @Query("SELECT COALESCE(SUM(sd.quantity), 0) FROM SaleDetail sd WHERE sd.sale.status = 'COMPLETED' AND sd.sale.createdAt >= :from AND sd.sale.createdAt < :to")
    long unidadesVendidas(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT sd.variant.product.category.name, COALESCE(SUM(sd.subtotal), 0)
            FROM SaleDetail sd
            WHERE sd.sale.status = 'COMPLETED' AND sd.sale.createdAt >= :from AND sd.sale.createdAt < :to
            GROUP BY sd.variant.product.category.id, sd.variant.product.category.name
            ORDER BY SUM(sd.subtotal) DESC
            """)
    List<Object[]> ventasPorCategoria(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT sd.productName, SUM(sd.quantity), COALESCE(SUM(sd.subtotal), 0)
            FROM SaleDetail sd
            WHERE sd.sale.status = 'COMPLETED' AND sd.sale.createdAt >= :from AND sd.sale.createdAt < :to
            GROUP BY sd.productName
            ORDER BY SUM(sd.quantity) DESC
            """)
    List<Object[]> topProductos(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
