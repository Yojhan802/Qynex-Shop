package com.freestyleperu.aplicacion.producto.service;

import com.freestyleperu.aplicacion.catalogo.domain.Brand;
import com.freestyleperu.aplicacion.catalogo.domain.Category;
import com.freestyleperu.aplicacion.catalogo.domain.Subcategory;
import com.freestyleperu.aplicacion.catalogo.repository.BrandRepository;
import com.freestyleperu.aplicacion.catalogo.repository.CategoryRepository;
import com.freestyleperu.aplicacion.catalogo.repository.SubcategoryRepository;
import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.dto.request.ActualizarProductoRequest;
import com.freestyleperu.aplicacion.producto.dto.request.CrearProductoRequest;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoDetalleResponse;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoResumenResponse;
import com.freestyleperu.aplicacion.producto.mapper.ProductoMapper;
import com.freestyleperu.aplicacion.producto.repository.ProductRepository;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.util.ImageUploadService;
import com.freestyleperu.aplicacion.shared.util.SequenceService;
import com.freestyleperu.aplicacion.shared.util.TextNormalizer;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final BrandRepository brandRepository;
    private final SequenceService sequenceService;
    private final ProductoMapper productoMapper;
    private final AuditService auditService;
    private final ImageUploadService imageUploadService;

    public ProductoService(ProductRepository productRepository, ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository, SubcategoryRepository subcategoryRepository,
            BrandRepository brandRepository, SequenceService sequenceService, ProductoMapper productoMapper,
            AuditService auditService, ImageUploadService imageUploadService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.brandRepository = brandRepository;
        this.sequenceService = sequenceService;
        this.productoMapper = productoMapper;
        this.auditService = auditService;
        this.imageUploadService = imageUploadService;
    }

    public Page<ProductoResumenResponse> listar(String search, Long categoryId, Long subcategoryId, Long brandId,
            EstadoGeneral status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Page<Product> page = productRepository.buscar(search, categoryId, subcategoryId, brandId, status, minPrice, maxPrice, pageable);
        return page.map(product -> productoMapper.toResumen(product, variantsDe(product.getId())));
    }

    public ProductoDetalleResponse obtener(Long id) {
        Product product = buscarOFallar(id);
        return productoMapper.toDetalle(product, variantsDe(product.getId()));
    }

    @Transactional
    public ProductoDetalleResponse crear(CrearProductoRequest request) {
        validarPrecios(request.price(), request.promoPrice());
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoría", request.categoryId()));
        Subcategory subcategory = resolverSubcategoria(request.subcategoryId(), category);
        Brand brand = resolverMarca(request.brandId());

        Product product = new Product();
        product.setCategory(category);
        product.setSubcategory(subcategory);
        product.setBrand(brand);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setMaterial(request.material());
        product.setFit(request.fit());
        product.setPrice(request.price());
        product.setPromoPrice(request.promoPrice());

        String prefix = TextNormalizer.prefix3(category.getName());
        product.setSku(codigoUnico(request.sku(), () -> sequenceService.next("SKU_" + prefix, prefix, 5), productRepository::existsBySku, "SKU"));
        product.setInternalCode(codigoUnico(request.internalCode(), () -> sequenceService.next("COD_" + prefix, prefix, 4),
                productRepository::existsByInternalCode, "código interno"));

        Product guardado = productRepository.save(product);
        auditService.log("PRODUCTO_CREADO", "PRODUCTO", guardado.getId(), null,
                new Object[] { guardado.getSku(), guardado.getName() }, AuditResult.SUCCESS);
        return productoMapper.toDetalle(guardado, List.of());
    }

    @Transactional
    public ProductoDetalleResponse actualizar(Long id, ActualizarProductoRequest request) {
        validarPrecios(request.price(), request.promoPrice());
        Product product = buscarOFallar(id);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Categoría", request.categoryId()));

        product.setCategory(category);
        product.setSubcategory(resolverSubcategoria(request.subcategoryId(), category));
        product.setBrand(resolverMarca(request.brandId()));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setMaterial(request.material());
        product.setFit(request.fit());
        product.setPrice(request.price());
        product.setPromoPrice(request.promoPrice());
        product.setImageUrl(request.imageUrl());

        auditService.log("PRODUCTO_ACTUALIZADO", "PRODUCTO", product.getId(), null, request, AuditResult.SUCCESS);
        return productoMapper.toDetalle(product, variantsDe(product.getId()));
    }

    @Transactional
    public ProductoResumenResponse cambiarEstado(Long id, EstadoGeneral status) {
        Product product = buscarOFallar(id);
        product.setStatus(status);
        auditService.log("PRODUCTO_CAMBIO_ESTADO", "PRODUCTO", product.getId(), null, status, AuditResult.SUCCESS);
        return productoMapper.toResumen(product, variantsDe(product.getId()));
    }

    @Transactional
    public ProductoDetalleResponse actualizarImagen(Long id, MultipartFile file) {
        Product product = buscarOFallar(id);
        product.setImageUrl(imageUploadService.guardar(file, "products"));
        auditService.log("PRODUCTO_IMAGEN_ACTUALIZADA", "PRODUCTO", product.getId(), null, product.getImageUrl(), AuditResult.SUCCESS);
        return productoMapper.toDetalle(product, variantsDe(product.getId()));
    }

    @Transactional
    public ProductoDetalleResponse actualizarGuiaTallas(Long id, MultipartFile file) {
        Product product = buscarOFallar(id);
        product.setSizeGuideImageUrl(imageUploadService.guardar(file, "size-guides"));
        auditService.log("PRODUCTO_GUIA_TALLAS_ACTUALIZADA", "PRODUCTO", product.getId(), null, product.getSizeGuideImageUrl(), AuditResult.SUCCESS);
        return productoMapper.toDetalle(product, variantsDe(product.getId()));
    }

    private void validarPrecios(BigDecimal price, BigDecimal promoPrice) {
        if (promoPrice != null && promoPrice.compareTo(price) >= 0) {
            throw new ReglaDeNegocioException("El precio promocional debe ser menor que el precio regular");
        }
    }

    private Subcategory resolverSubcategoria(Long subcategoryId, Category category) {
        if (subcategoryId == null) {
            return null;
        }
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Subcategoría", subcategoryId));
        if (!subcategory.getCategory().getId().equals(category.getId())) {
            throw new ReglaDeNegocioException("La subcategoría no pertenece a la categoría seleccionada");
        }
        return subcategory;
    }

    private Brand resolverMarca(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return brandRepository.findById(brandId).orElseThrow(() -> RecursoNoEncontradoException.de("Marca", brandId));
    }

    private String codigoUnico(String provisto, java.util.function.Supplier<String> generador,
            java.util.function.Predicate<String> existe, String etiqueta) {
        if (provisto == null || provisto.isBlank()) {
            return generador.get();
        }
        if (existe.test(provisto)) {
            throw new RecursoDuplicadoException("Ya existe un producto con ese " + etiqueta + ": " + provisto);
        }
        return provisto;
    }

    private List<ProductVariant> variantsDe(Long productId) {
        return variantRepository.findAllByProductIdOrderBySizeSortOrderAscColorNameAsc(productId);
    }

    private Product buscarOFallar(Long id) {
        return productRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Producto", id));
    }
}
