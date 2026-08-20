package com.freestyleperu.aplicacion.devolucion.repository;

import com.freestyleperu.aplicacion.devolucion.domain.ReturnDetail;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnDetailRepository extends JpaRepository<ReturnDetail, Long> {

    @EntityGraph(attributePaths = { "variant", "saleDetail" })
    List<ReturnDetail> findAllByReturnEntityId(Long returnId);

    @Query("SELECT COALESCE(SUM(rd.quantity), 0) FROM ReturnDetail rd WHERE rd.saleDetail.id = :saleDetailId")
    int cantidadDevueltaPorLinea(@Param("saleDetailId") Long saleDetailId);
}
