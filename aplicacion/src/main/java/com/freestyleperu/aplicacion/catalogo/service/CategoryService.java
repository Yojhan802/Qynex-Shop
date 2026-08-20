package com.freestyleperu.aplicacion.catalogo.service;

import com.freestyleperu.aplicacion.catalogo.domain.Category;
import com.freestyleperu.aplicacion.catalogo.dto.request.CategoryRequest;
import com.freestyleperu.aplicacion.catalogo.dto.response.CategoryResponse;
import com.freestyleperu.aplicacion.catalogo.repository.CategoryRepository;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.util.TextNormalizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> listar() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public CategoryResponse obtener(Long id) {
        return toResponse(buscarOFallar(id));
    }

    @Transactional
    public CategoryResponse crear(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new RecursoDuplicadoException("Ya existe una categoría llamada " + request.name());
        }
        Category category = new Category();
        category.setName(request.name());
        category.setSlug(slugUnico(request.name()));
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse actualizar(Long id, CategoryRequest request) {
        Category category = buscarOFallar(id);
        if (!category.getName().equalsIgnoreCase(request.name()) && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new RecursoDuplicadoException("Ya existe una categoría llamada " + request.name());
        }
        category.setName(request.name());
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse cambiarEstado(Long id, EstadoGeneral status) {
        Category category = buscarOFallar(id);
        category.setStatus(status);
        return toResponse(category);
    }

    private String slugUnico(String name) {
        String base = TextNormalizer.slugify(name);
        String slug = base;
        int suffix = 2;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private Category buscarOFallar(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Categoría", id));
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getStatus());
    }
}
