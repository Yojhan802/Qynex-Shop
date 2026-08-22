package com.freestyleperu.aplicacion.tienda.service;

import com.freestyleperu.aplicacion.catalogo.repository.BrandRepository;
import com.freestyleperu.aplicacion.catalogo.repository.CategoryRepository;
import com.freestyleperu.aplicacion.configuracion.service.ConfiguracionService;
import com.freestyleperu.aplicacion.pago.repository.PaymentMethodRepository;
import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.repository.ProductRepository;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.promocion.service.PromocionService;
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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura pública del catálogo — nunca expone SKU/código interno/barcode/stock
 * exacto (ver plan Fase 2). En un pico de tráfico (ej. Black Friday) esta es
 * la ruta más pegada — se cachea unos segundos (`storeCatalog`,
 * `spring.cache.caffeine.spec`) para no golpear la base de datos por cada
 * visita. El checkout (`PedidoService.crear`) nunca lee de esta caché: valida
 * stock siempre contra la base en el momento, así que un dato de "disponible"
 * con unos segundos de retraso nunca permite vender de más, a lo sumo muestra
 * "sin stock" un momento después de lo ideal.
 */
@Service
@Transactional(readOnly = true)
public class TiendaCatalogoService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final ConfiguracionService configuracionService;
    private final PromocionService promocionService;

    private static final String DISTRITO_ENVIO_GRATIS = "Huacho";

    public TiendaCatalogoService(ProductRepository productRepository, ProductVariantRepository variantRepository,
            CategoryRepository categoryRepository, BrandRepository brandRepository,
            PaymentMethodRepository paymentMethodRepository, ConfiguracionService configuracionService,
            PromocionService promocionService) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.configuracionService = configuracionService;
        this.promocionService = promocionService;
    }

    @Cacheable("storeCatalogProducts")
    public PageResponse<PublicProductoResumenResponse> listarProductos(
            String search, Long categoryId, Long brandId, Pageable pageable) {
        return PageResponse.of(
                productRepository.buscar(search, categoryId, null, brandId, EstadoGeneral.ACTIVE, null, null, pageable),
                this::toResumen);
    }

    @Cacheable("storeCatalogProductDetail")
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
                product.getPrice(), promoPriceParaMostrar(product), product.getImageUrl(), product.getSizeGuideImageUrl(),
                product.getCategory().getName(), product.getBrand() != null ? product.getBrand().getName() : null, variantes);
    }

    /**
     * El {@code promoPrice} que se expone al público reutiliza el mismo campo
     * de siempre: si una promoción marcada {@code visibleOnline} deja un
     * precio menor al normal, se muestra esa — si no, el {@code promoPrice}
     * estático del producto (o nulo si tampoco hay). El frontend de la
     * tienda no necesita saber nada de promociones, solo sigue leyendo
     * {@code promoPrice} como ya lo hacía.
     */
    private BigDecimal promoPriceParaMostrar(Product product) {
        BigDecimal efectivo = promocionService.precioEfectivoOnline(product);
        return efectivo.compareTo(product.getPrice()) < 0 ? efectivo : null;
    }

    @Cacheable("storeCatalogCategories")
    public List<PublicCategoriaResponse> listarCategorias() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getStatus() == EstadoGeneral.ACTIVE)
                .map(c -> new PublicCategoriaResponse(c.getId(), c.getName(), c.getSlug()))
                .toList();
    }

    @Cacheable("storeCatalogBrands")
    public List<PublicMarcaResponse> listarMarcas() {
        return brandRepository.findAllByOrderByNameAsc().stream()
                .filter(b -> b.getStatus() == EstadoGeneral.ACTIVE)
                .map(b -> new PublicMarcaResponse(b.getId(), b.getName()))
                .toList();
    }

    @Cacheable("storeCatalogShipping")
    public PublicShippingInfoResponse obtenerInfoEnvio() {
        return new PublicShippingInfoResponse(configuracionService.obtenerTarifaEnvio(), DISTRITO_ENVIO_GRATIS);
    }

    @Cacheable("storeCatalogPaymentMethods")
    public List<PublicMetodoPagoResponse> listarMetodosPago() {
        // affectsCash = true (ej. EFECTIVO) no tiene sentido en un checkout online sin cajero
        // presente — mismo criterio que ya usa la seña de separaciones (RN-27).
        return paymentMethodRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(m -> m.getStatus() == EstadoGeneral.ACTIVE && !m.isAffectsCash())
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
                product.getId(), product.getName(), product.getPrice(), promoPriceParaMostrar(product), product.getImageUrl(),
                product.getCategory().getName(), product.getBrand() != null ? product.getBrand().getName() : null,
                colores, inStock);
    }

    private PublicVarianteResponse toVariante(ProductVariant variant) {
        return new PublicVarianteResponse(
                variant.getId(), variant.getColor().getId(), variant.getColor().getName(), variant.getColor().getHexCode(),
                variant.getSize().getId(), variant.getSize().getName(), variant.getStock() > 0);
    }
}
