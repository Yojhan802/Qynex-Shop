package com.freestyleperu.aplicacion.pedido.repository;

import com.freestyleperu.aplicacion.pedido.domain.PedidoDetail;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoDetailRepository extends JpaRepository<PedidoDetail, Long> {

    @EntityGraph(attributePaths = { "variant" })
    List<PedidoDetail> findAllByPedidoId(Long pedidoId);
}
