package com.freestyleperu.aplicacion.inventario.repository;

import com.freestyleperu.aplicacion.inventario.domain.Warehouse;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findFirstByStatusOrderByIdAsc(EstadoGeneral status);
}
