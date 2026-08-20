package com.freestyleperu.aplicacion.catalogo.service;

import com.freestyleperu.aplicacion.catalogo.domain.Color;
import com.freestyleperu.aplicacion.catalogo.dto.request.ColorRequest;
import com.freestyleperu.aplicacion.catalogo.dto.response.ColorResponse;
import com.freestyleperu.aplicacion.catalogo.repository.ColorRepository;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ColorService {

    private final ColorRepository colorRepository;

    public ColorService(ColorRepository colorRepository) {
        this.colorRepository = colorRepository;
    }

    public List<ColorResponse> listar() {
        return colorRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public ColorResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public ColorResponse crear(ColorRequest request) {
        if (colorRepository.existsByNameIgnoreCase(request.name())) {
            throw new RecursoDuplicadoException("Ya existe un color llamado " + request.name());
        }
        Color color = new Color();
        color.setName(request.name());
        color.setHexCode(request.hexCode());
        return toResponse(colorRepository.save(color));
    }

    @Transactional
    public ColorResponse actualizar(Long id, ColorRequest request) {
        Color color = buscarOFallar(id);
        if (!color.getName().equalsIgnoreCase(request.name()) && colorRepository.existsByNameIgnoreCase(request.name())) {
            throw new RecursoDuplicadoException("Ya existe un color llamado " + request.name());
        }
        color.setName(request.name());
        color.setHexCode(request.hexCode());
        return toResponse(color);
    }

    @Transactional
    public ColorResponse cambiarEstado(Long id, EstadoGeneral status) {
        Color color = buscarOFallar(id);
        color.setStatus(status);
        return toResponse(color);
    }

    private Color buscarOFallar(Long id) {
        return colorRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Color", id));
    }

    private ColorResponse toResponse(Color color) {
        return new ColorResponse(color.getId(), color.getName(), color.getHexCode(), color.getStatus());
    }
}
