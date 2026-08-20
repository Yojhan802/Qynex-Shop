package com.freestyleperu.aplicacion.venta.repository;

import com.freestyleperu.aplicacion.venta.domain.Payment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "paymentMethod")
    List<Payment> findAllBySaleId(Long saleId);

    @Query("""
            SELECT p.paymentMethod.name, COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.sale.status = 'COMPLETED' AND p.createdAt >= :from AND p.createdAt < :to
            GROUP BY p.paymentMethod.id, p.paymentMethod.name
            ORDER BY SUM(p.amount) DESC
            """)
    List<Object[]> distribucionPorMetodo(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
