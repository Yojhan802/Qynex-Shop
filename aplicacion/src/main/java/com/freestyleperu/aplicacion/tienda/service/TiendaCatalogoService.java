package com.freestyleperu.aplicacion.tienda.service;

import com.freestyleperu.aplicacion.catalogo.repository.BrandRepository;
import com.freestyleperu.aplicacion.catalogo.repository.CategoryRepository;
import com.freestyleperu.aplicacion.configuracion.service.ConfiguracionService;
import com.freestyleperu.aplicacion.pago.repository.PaymentMethodRepository;
import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.repository.ProductRepository;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicCategoriaResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicColorSwatchResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicMarcaResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicMetodoPagoResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicProductoDetalleResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicProductoResumenResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicShippingInfoResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicVarianteResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura pública del catálogo — nunca expone SKU/código interno/barcode/stock exacto (ver plan Fase 2). */
@Service
@Transactional(readOnly = true)
public class TiendaCatalogoService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ConfiguracionService configuracionService;

    private static final String DISTRITO_ENVIO_GRATIS = "Huacho";

    public TiendaCatalogoService(ProductRepository productRepository, ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository, BrandRepository brandRepository,
            PaymentMethodRepository paymentMethodRepository, ConfiguracionService configuracionService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.configuracionService = configuracionService;
    }

    public PageResponse<PublicProductoResumenResponse> listarProductos(
            String search, Long categoryId, Long brandId, Pageable pageable) {
        return PageResponse.of(
                productRepository.buscar(search, categoryId, null, brandId, EstadoGeneral.ACTIVE, null, null, pageable),
                this::toResumen);
    }

    public PublicProductoDetalleResponse obtenerProducto(Long id) {
        Product product = productRepository.findById(id)
                .filter(p -> p.getStatus() == EstadoGeneral.ACTIVE)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Producto", id));

        List<PublicVarianteResponse> variantes = variantRepository.findAllByProductIdOrderBySizeSortOrderAscColorNameAsc(id).stream()
                .filter(v -> v.getStatus() == EstadoGeneral.ACTIVE)
                .map(this::toVariante)
                .toList();

        return new PublicProductoDetalleResponse(
                product.getId(), product.getName(), product.getDescription(), product.getMaterial(), product.getFit(),
                product.getPrice(), product.getPromoPrice(), product.getImageUrl(), product.getSizeGuideImageUrl(),
                product.getCategory().getName(), product.getBrand() != null ? product.getBrand().getName() : null, variantes);
    }

    public List<PublicCategoriaResponse> listarCategorias() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getStatus() == EstadoGeneral.ACTIVE)
                .map(c -> new PublicCategoriaResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    public List<PublicMarcaResponse> listarMarcas() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .filter(b -> b.getStatus() == EstadoGeneral.ACTIVE)
                .map(b -> new PublicMarcaResponse(b.getId(), b.getName()))
                .toList();
    }

    public PublicShippingInfoResponse obtenerInfoEnvio() {
        return new PublicShippingInfoResponse(configuracionService.obtenerTarifaEnvio(), DISTRITO_ENVIO_GRATIS);
    }

    public List<PublicMetodoPagoResponse> listarMetodosPago() {
        return paymentMethodRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(m -> m.getStatus() == EstadoGeneral.ACTIVE)
                .map(m -> new PublicMetodoPagoResponse(
                        m.getId(), m.getCode(), m.getName(), m.getType(), m.isRequiresReference(),
                        m.getAccountHolder(), m.getAccountNumber(), m.getQrImageUrl()))
                .toList();
    }

    private static final int MAX_SWATCHES = 6;

    private PublicProductoResumenResponse toResumen(Product product) {
        List<ProductVariant> variantesActivas = variantRepository
                .findAllByProductIdOrderBySizeSortOrderAscColorNameAsc(product.getId()).stream()
                .filter(v -> v.getStatus() == EstadoGeneral.ACTIVE)
                .toList();
        boolean inStock = variantesActivas.stream().anyMatch(v -> v.getStock() > 0);

        List<PublicColorSwatchResponse> colores = variantesActivas.stream()
                .collect(Collectors.toMap(
                        v -> v.getColor().getId(), v -> v.getColor(), (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .map(color -> new PublicColorSwatchResponse(color.getName(), color.getHexCode()))
                .limit(MAX_SWATCHES)
                .toList();

        return new PublicProductoResumenResponse(
                product.getId(), product.getName(), product.getPrice(), product.getPromoPrice(), product.getImageUrl(),
                product.getCategory().getName(), product.getBrand() != null ? product.getBrand().getName() : null,
                colores, inStock);
    }

    private PublicVarianteResponse toVariante(ProductVariant variant) {
        return new PublicVarianteResponse(
                variant.getId(), variant.getColor().getId(), variant.getColor().getName(), variant.getColor().getHexCode(),
                variant.getSize().getId(), variant.getSize().getName(), variant.getStock() > 0);
    }
}
