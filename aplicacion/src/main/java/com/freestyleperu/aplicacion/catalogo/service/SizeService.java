package com.freestyleperu.aplicacion.catalogo.service;

import com.freestyleperu.aplicacion.catalogo.domain.Size;
import com.freestyleperu.aplicacion.catalogo.dto.request.SizeRequest;
import com.freestyleperu.aplicacion.catalogo.dto.response.SizeResponse;
import com.freestyleperu.aplicacion.catalogo.repository.SizeRepository;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SizeService {

    private final SizeRepository sizeRepository;

    public SizeService(SizeRepository sizeRepository) {
        this.sizeRepository = sizeRepository;
    }

    public List<SizeResponse> listar() {
        return sizeRepository.findAllByOrderBySortOrderAsc().stream().map(this::toResponse).toList();
    }

    public SizeResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public SizeResponse crear(SizeRequest request) {
        if (sizeRepository.existsByNameIgnoreCase(request.name())) {
            throw new RecursoDuplicadoException("Ya existe una talla llamada " + request.name());
        }
        Size size = new Size();
        size.setName(request.name());
        size.setSortOrder(request.sortOrder());
        return toResponse(sizeRepository.save(size));
    }

    @Transactional
    public SizeResponse actualizar(Long id, SizeRequest request) {
        Size size = buscarOFallar(id);
        if (!size.getName().equalsIgnoreCase(request.name()) && sizeRepository.existsByNameIgnoreCase(request.name())) {
            throw new RecursoDuplicadoException("Ya existe una talla llamada " + request.name());
        }
        size.setName(request.name());
        size.setSortOrder(request.sortOrder());
        return toResponse(size);
    }

    @Transactional
    public SizeResponse cambiarEstado(Long id, EstadoGeneral status) {
        Size size = buscarOFallar(id);
        size.setStatus(status);
        return toResponse(size);
    }

    private Size buscarOFallar(Long id) {
        return sizeRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Talla", id));
    }

    private SizeResponse toResponse(Size size) {
        return new SizeResponse(size.getId(), size.getName(), size.getSortOrder(), size.getStatus());
    }
}
