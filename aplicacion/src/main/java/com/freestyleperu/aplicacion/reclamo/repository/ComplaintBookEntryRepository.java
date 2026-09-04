package com.freestyleperu.aplicacion.reclamo.repository;

import com.freestyleperu.aplicacion.reclamo.domain.ComplaintBookEntry;
import com.freestyleperu.aplicacion.reclamo.domain.ComplaintStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintBookEntryRepository extends JpaRepository<ComplaintBookEntry, Long> {

    @EntityGraph(attributePaths = "respondedBy")
    Page<ComplaintBookEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "respondedBy")
    Optional<ComplaintBookEntry> findByEntryNumber(String entryNumber);

    /**
     * Hojas todavía sin responder, para el aviso de plazo. Filtra por @TenantId como
     * cualquier consulta, así que quien la llame desde un job debe fijar TenantContext.
     */
    List<ComplaintBookEntry> findAllByStatusOrderByCreatedAtAsc(ComplaintStatus status);
}
