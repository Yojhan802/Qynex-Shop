package com.freestyleperu.aplicacion.inventario.repository;

import com.freestyleperu.aplicacion.inventario.domain.Branch;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findFirstByStatusOrderByIdAsc(EstadoGeneral status);
}
