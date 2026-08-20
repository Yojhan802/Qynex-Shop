package com.freestyleperu.aplicacion.catalogo.repository;

import com.freestyleperu.aplicacion.catalogo.domain.Subcategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    List<Subcategory> findAllByCategoryIdOrderByNameAsc(Long categoryId);

    List<Subcategory> findAllByOrderByNameAsc();
}
