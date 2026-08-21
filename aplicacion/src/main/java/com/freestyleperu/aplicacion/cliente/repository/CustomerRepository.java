package com.freestyleperu.aplicacion.cliente.repository;

import com.freestyleperu.aplicacion.cliente.domain.Customer;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByDocNumber(String docNumber);

    Optional<Customer> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:search IS NULL
                   OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR c.docNumber LIKE CONCAT('%', :search, '%')
                   OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:status IS NULL OR c.status = :status)
            """)
    Page<Customer> buscar(@Param("search") String search, @Param("status") EstadoGeneral status, Pageable pageable);

    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR c.docNumber LIKE CONCAT('%', :query, '%')
            """)
    List<Customer> buscarRapido(@Param("query") String query);
}
