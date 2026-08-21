package com.freestyleperu.aplicacion.promotor.repository;

import com.freestyleperu.aplicacion.promotor.domain.Promoter;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoterRepository extends JpaRepository<Promoter, Long> {

    List<Promoter> findAllByOrderByNameAsc();
}
