package com.freestyleperu.aplicacion.catalogo.repository;

import com.freestyleperu.aplicacion.catalogo.domain.Size;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SizeRepository extends JpaRepository<Size, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Size> findAllByOrderBySortOrderAsc();
}
