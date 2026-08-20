package com.freestyleperu.aplicacion.producto.service;

import com.freestyleperu.aplicacion.catalogo.domain.Color;
import com.freestyleperu.aplicacion.catalogo.domain.Size;
import com.freestyleperu.aplicacion.catalogo.repository.ColorRepository;
import com.freestyleperu.aplicacion.catalogo.repository.SizeRepository;
import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.dto.request.ActualizarVarianteRequest;
import com.freestyleperu.aplicacion.producto.dto.request.CrearVarianteRequest;
import com.freestyleperu.aplicacion.producto.dto.request.GenerarVariantesRequest;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteBusquedaResponse;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteResponse;
import com.freestyleperu.aplicacion.producto.mapper.VarianteMapper;
import com.freestyleperu.aplicacion.producto.repository.ProductRepository;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.StockInsuficienteException;
import com.freestyleperu.aplicacion.shared.util.Ean13Generator;
import com.freestyleperu.aplicacion.shared.util.TextNormalizer;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VarianteService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final Ean13Generator ean13Generator;
    private final VarianteMapper varianteMapper;
    private final AuditService auditService;

    public VarianteService(ProductVariantRepository variantRepository, ProductRepository productRepository,
            ColorRepository colorRepository, SizeRepository sizeRepository, Ean13Generator ean13Generator,
            VarianteMapper varianteMapper, AuditService auditService) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
        this.colorRepository = colorRepository;
        this.sizeRepository = sizeRepository;
        this.ean13Generator = ean13Generator;
        this.varianteMapper = varianteMapper;
        this.auditService = auditService;
    }

    public List<VarianteResponse> listarPorProducto(Long productId) {
        return variantRepository.findAllByProductIdOrderBySizeSortOrderAscColorNameAsc(productId).stream()
                .map(varianteMapper::toResponse)
                .toList();
    }

    @Transactional
    public VarianteResponse crear(Long productId, CrearVarianteRequest request) {
        Product product = buscarProductoOFallar(productId);
        Color color = buscarColorOFallar(request.colorId());
        Size size = buscarTallaOFallar(request.sizeId());

        ProductVariant variant = construirVariante(product, color, size, request.sku(), request.barcode(),
                request.generateBarcode(), request.stock(), request.minStock());

        ProductVariant guardado = variantRepository.save(variant);
        auditService.log("VARIANTE_CREADA", "VARIANTE", guardado.getId(), null,
                new Object[] { guardado.getSku(), guardado.getBarcode() }, AuditResult.SUCCESS);
        return varianteMapper.toResponse(guardado);
    }

    /**
     * Genera la matriz color × talla para un producto. Las combinaciones que
     * ya existan se omiten sin fallar (semántica idempotente, ver docs/05-api.md §6).
     */
    @Transactional
    public List<VarianteResponse> generarMatriz(Long productId, GenerarVariantesRequest request) {
        Product product = buscarProductoOFallar(productId);
        List<Color> colors = colorRepository.findAllById(request.colorIds());
        List<Size> sizes = sizeRepository.findAllById(request.sizeIds());
        if (colors.size() != request.colorIds().size()) {
            throw new RecursoNoEncontradoException("Uno o más colores no existen");
        }
        if (sizes.size() != request.sizeIds().size()) {
            throw new RecursoNoEncontradoException("Una o más tallas no existen");
        }

        int minStock = request.minStock() != null ? request.minStock() : 0;
        List<ProductVariant> creadas = new java.util.ArrayList<>();
        for (Color color : colors) {
            for (Size size : sizes) {
                if (variantRepository.existsByProductIdAndColorIdAndSizeId(productId, color.getId(), size.getId())) {
                    continue;
                }
                ProductVariant variant = construirVariante(product, color, size, null, null,
                        request.generateBarcodes(), 0, minStock);
                creadas.add(variantRepository.save(variant));
            }
        }

        auditService.log("VARIANTES_GENERADAS", "PRODUCTO", productId, null, creadas.size(), AuditResult.SUCCESS);
        return creadas.stream().map(varianteMapper::toResponse).toList();
    }

    @Transactional
    public VarianteResponse actualizar(Long id, ActualizarVarianteRequest request) {
        ProductVariant variant = buscarVarianteOFallar(id);
        variant.setMinStock(request.minStock());
        auditService.log("VARIANTE_ACTUALIZADA", "VARIANTE", variant.getId(), null, request, AuditResult.SUCCESS);
        return varianteMapper.toResponse(variant);
    }

    @Transactional
    public VarianteResponse cambiarEstado(Long id, EstadoGeneral status) {
        ProductVariant variant = buscarVarianteOFallar(id);
        variant.setStatus(status);
        auditService.log("VARIANTE_CAMBIO_ESTADO", "VARIANTE", variant.getId(), null, status, AuditResult.SUCCESS);
        return varianteMapper.toResponse(variant);
    }

    @Transactional
    public VarianteResponse asignarCodigoBarras(Long id) {
        ProductVariant variant = buscarVarianteOFallar(id);
        variant.setBarcode(ean13Generator.generar());
        auditService.log("BARCODE_ASIGNADO", "VARIANTE", variant.getId(), null, variant.getBarcode(), AuditResult.SUCCESS);
        return varianteMapper.toResponse(variant);
    }

    public VarianteBusquedaResponse buscarPorCodigoBarras(String barcode) {
        ProductVariant variant = variantRepository.findByBarcode(barcode)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró ninguna variante con el código " + barcode));
        return varianteMapper.toBusquedaResponse(variant);
    }

    public List<VarianteBusquedaResponse> buscar(String query) {
        return variantRepository.buscar(query).stream().map(varianteMapper::toBusquedaResponse).toList();
    }

    private ProductVariant construirVariante(Product product, Color color, Size size, String skuProvisto,
            String barcodeProvisto, boolean generarBarcode, Integer stock, Integer minStock) {
        if (variantRepository.existsByProductIdAndColorIdAndSizeId(product.getId(), color.getId(), size.getId())) {
            throw new RecursoDuplicadoException(
                    "Ya existe la variante " + color.getName() + " / " + size.getName() + " para este producto");
        }

        String sku = skuProvisto != null && !skuProvisto.isBlank() ? skuProvisto : skuGenerado(product, color, size);
        if (variantRepository.existsBySku(sku)) {
            throw new RecursoDuplicadoException("Ya existe una variante con el SKU " + sku);
        }

        String barcode = barcodeProvisto;
        if (barcode != null && !barcode.isBlank()) {
            if (variantRepository.existsByBarcode(barcode)) {
                throw new RecursoDuplicadoException("El código de barras " + barcode + " ya está registrado");
            }
        } else if (generarBarcode) {
            barcode = ean13Generator.generar();
        } else {
            barcode = null;
        }

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setColor(color);
        variant.setSize(size);
        variant.setSku(sku);
        variant.setBarcode(barcode);
        variant.setStock(stock != null ? stock : 0);
        variant.setMinStock(minStock != null ? minStock : 0);
        return variant;
    }

    private String skuGenerado(Product product, Color color, Size size) {
        return product.getSku() + "-" + size.getName().toUpperCase() + "-" + TextNormalizer.prefix3(color.getName());
    }

    private Product buscarProductoOFallar(Long id) {
        return productRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Producto", id));
    }

    private Color buscarColorOFallar(Long id) {
        return colorRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Color", id));
    }

    private Size buscarTallaOFallar(Long id) {
        return sizeRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Talla", id));
    }

    private ProductVariant buscarVarianteOFallar(Long id) {
        return variantRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Variante", id));
    }

    /**
     * Único punto autorizado a escribir {@code product_variants.stock} (RN-05,
     * RN-06). Nunca se expone por un controller: solo {@code InventarioService}
     * lo invoca, siempre junto con el movimiento que justifica el cambio, y
     * dentro de la misma transacción (por eso exige {@code MANDATORY}).
     * El bloqueo pesimista evita que dos ajustes concurrentes sobre la misma
     * variante calculen el mismo "stock antes" a la vez.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public AjusteStockResultado ajustarStock(Long variantId, int delta) {
        ProductVariant variant = variantRepository.lockById(variantId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Variante", variantId));
        int stockBefore = variant.getStock();
        int stockAfter = stockBefore + delta;
        if (stockAfter < 0) {
            throw new StockInsuficienteException(
                    "Stock insuficiente para " + variant.getProduct().getName() + " " + variant.getColor().getName()
                            + " " + variant.getSize().getName() + ". Disponible: " + stockBefore + ", solicitado: " + (-delta));
        }
        variant.setStock(stockAfter);
        return new AjusteStockResultado(variant.getId(), variant.getSku(), variant.getProduct().getName(), stockBefore, stockAfter);
    }

    /** Proxy liviano (sin cargar datos) para que otros módulos enlacen la FK en sus propias entidades. */
    @Transactional(propagation = Propagation.MANDATORY)
    public ProductVariant referencia(Long id) {
        return variantRepository.getReferenceById(id);
    }
}
