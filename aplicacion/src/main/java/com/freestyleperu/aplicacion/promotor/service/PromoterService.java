package com.freestyleperu.aplicacion.promotor.service;

import com.freestyleperu.aplicacion.promotor.domain.Promoter;
import com.freestyleperu.aplicacion.promotor.dto.request.PromoterRequest;
import com.freestyleperu.aplicacion.promotor.dto.response.PromoterResponse;
import com.freestyleperu.aplicacion.promotor.repository.PromoterRepository;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PromoterService {

    private final PromoterRepository promoterRepository;

    public PromoterService(PromoterRepository promoterRepository) {
        this.promoterRepository = promoterRepository;
    }

    public List<PromoterResponse> listar() {
        return promoterRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    /** Usado por el módulo de ventas al registrar una venta con promotor opcional. */
    public Promoter obtenerActivoOFallar(Long id) {
        Promoter promoter = buscarOFallar(id);
        if (promoter.getStatus() != EstadoGeneral.ACTIVE) {
            throw new RecursoNoEncontradoException("El promotor " + promoter.getName() + " no está disponible");
        }
        return promoter;
    }

    @Transactional
    public PromoterResponse crear(PromoterRequest request) {
        Promoter promoter = new Promoter();
        promoter.setName(request.name());
        return toResponse(promoterRepository.save(promoter));
    }

    @Transactional
    public PromoterResponse actualizar(Long id, PromoterRequest request) {
        Promoter promoter = buscarOFallar(id);
        promoter.setName(request.name());
        return toResponse(promoter);
    }

    @Transactional
    public PromoterResponse cambiarEstado(Long id, EstadoGeneral status) {
        Promoter promoter = buscarOFallar(id);
        promoter.setStatus(status);
        return toResponse(promoter);
    }

    private Promoter buscarOFallar(Long id) {
        return promoterRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Promotor", id));
    }

    private PromoterResponse toResponse(Promoter promoter) {
        return new PromoterResponse(promoter.getId(), promoter.getName(), promoter.getStatus());
    }
}
