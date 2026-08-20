package com.freestyleperu.aplicacion.catalogo.repository;

import com.freestyleperu.aplicacion.catalogo.domain.Color;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ColorRepository extends JpaRepository<Color, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Color> findAllByOrderByNameAsc();
}
