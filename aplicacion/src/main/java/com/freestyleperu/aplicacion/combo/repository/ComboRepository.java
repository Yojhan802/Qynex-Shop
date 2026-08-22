package com.freestyleperu.aplicacion.combo.repository;

import com.freestyleperu.aplicacion.combo.domain.Combo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComboRepository extends JpaRepository<Combo, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = { "items", "items.product", "items.category", "items.brand" })
    List<Combo> findAllByOrderByNameAsc();

    @Override
    @EntityGraph(attributePaths = { "items", "items.product", "items.category", "items.brand" })
    Optional<Combo> findById(Long id);
}
