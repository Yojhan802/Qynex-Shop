package com.freestyleperu.aplicacion.usuario.repository;

import com.freestyleperu.aplicacion.usuario.domain.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Long> {

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = "permisos")
    Optional<Rol> findWithPermisosById(Long id);

    @EntityGraph(attributePaths = "permisos")
    List<Rol> findAllByOrderByNameAsc();
}
