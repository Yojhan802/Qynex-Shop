package com.freestyleperu.aplicacion.usuario.repository;

import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = { "roles", "roles.permisos" })
    Optional<Usuario> findByUsername(String username);

    @EntityGraph(attributePaths = { "roles", "roles.permisos" })
    Optional<Usuario> findWithRolesById(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByDni(String dni);

    @Query("""
            SELECT u FROM Usuario u
            WHERE (:search IS NULL
                   OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<Usuario> buscar(@Param("search") String search, @Param("status") UsuarioEstado status, Pageable pageable);
}
