package com.freestyleperu.aplicacion.pedido.service;

import com.freestyleperu.aplicacion.cliente.domain.Customer;
import com.freestyleperu.aplicacion.cliente.repository.CustomerRepository;
import com.freestyleperu.aplicacion.configuracion.service.ConfiguracionService;
import com.freestyleperu.aplicacion.inventario.service.InventarioService;
import com.freestyleperu.aplicacion.pago.domain.PaymentMethod;
import com.freestyleperu.aplicacion.pago.service.PagoService;
import com.freestyleperu.aplicacion.pedido.domain.Pedido;
import com.freestyleperu.aplicacion.pedido.domain.PedidoDetail;
import com.freestyleperu.aplicacion.pedido.domain.PedidoStatus;
import com.freestyleperu.aplicacion.pedido.dto.request.CancelarPedidoRequest;
import com.freestyleperu.aplicacion.pedido.dto.request.CrearPedidoRequest;
import com.freestyleperu.aplicacion.pedido.dto.request.ItemPedidoRequest;
import com.freestyleperu.aplicacion.pedido.dto.response.PedidoItemResponse;
import com.freestyleperu.aplicacion.pedido.dto.response.PedidoResponse;
import com.freestyleperu.aplicacion.pedido.dto.response.PedidoResumenResponse;
import com.freestyleperu.aplicacion.pedido.repository.PedidoDetailRepository;
import com.freestyleperu.aplicacion.pedido.repository.PedidoRepository;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.exception.StockInsuficienteException;
import com.freestyleperu.aplicacion.shared.util.ImageUploadService;
import com.freestyleperu.aplicacion.shared.util.SequenceService;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Pedidos de la tienda online. El stock no se toca al crear el pedido, solo
 * se valida como referencia — recién se descuenta cuando el staff confirma
 * el pago (ver docs/03-modelo-datos.md, Fase 2): el pago es manual, así que
 * "pedido creado" todavía no significa "pago recibido".
 */
@Service
@Transactional(readOnly = true)
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PedidoDetailRepository pedidoDetailRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final PagoService pagoService;
    private final ConfiguracionService configuracionService;
    private final SequenceService sequenceService;
    private final AuditService auditService;
    private final ImageUploadService imageUploadService;

    private static final String DISTRITO_CONTRAENTREGA = "Huacho";
    private static final String CODIGO_CONTRAENTREGA = "CONTRAENTREGA";

    public PedidoService(PedidoRepository pedidoRepository, PedidoDetailRepository pedidoDetailRepository,
            ProductVariantRepository variantRepository, CustomerRepository customerRepository,
            UsuarioRepository usuarioRepository, InventarioService inventarioService, PagoService pagoService,
            ConfiguracionService configuracionService, SequenceService sequenceService, AuditService auditService,
            ImageUploadService imageUploadService) {
        this.pedidoRepository = pedidoRepository;
        this.pedidoDetailRepository = pedidoDetailRepository;
        this.variantRepository = variantRepository;
        this.customerRepository = customerRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.pagoService = pagoService;
        this.configuracionService = configuracionService;
        this.sequenceService = sequenceService;
        this.auditService = auditService;
        this.imageUploadService = imageUploadService;
    }

    public PageResponse<PedidoResumenResponse> listar(PedidoStatus status, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return PageResponse.of(pedidoRepository.buscar(null, status, from, to, pageable), this::toResumen);
    }

    public PedidoResponse obtener(Long id) {
        Pedido pedido = buscarOFallar(id);
        return toResponse(pedido, pedidoDetailRepository.findAllByPedidoId(id));
    }

    public PageResponse<PedidoResumenResponse> listarPropios(Long customerId, Pageable pageable) {
        return PageResponse.of(pedidoRepository.buscar(customerId, null, null, null, pageable), this::toResumen);
    }

    public PedidoResponse obtenerPropio(Long id, Long customerId) {
        Pedido pedido = buscarOFallar(id);
        if (!pedido.getCustomer().getId().equals(customerId)) {
            throw RecursoNoEncontradoException.de("Pedido", id);
        }
        return toResponse(pedido, pedidoDetailRepository.findAllByPedidoId(id));
    }

    @Transactional
    public PedidoResponse crear(CrearPedidoRequest request, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", customerId));
        PaymentMethod paymentMethod = pagoService.obtenerActivoOFallar(request.paymentMethodId());

        List<ItemPedidoRequest> itemsOrdenados = request.items().stream()
                .sorted(Comparator.comparing(ItemPedidoRequest::variantId))
                .toList();

        Map<Long, ProductVariant> variantesPorId = cargarVariantes(itemsOrdenados);
        List<DetalleCalculado> detalles = new ArrayList<>();
        for (ItemPedidoRequest item : itemsOrdenados) {
            ProductVariant variant = variantesPorId.get(item.variantId());
            if (variant.getStock() < item.quantity()) {
                throw new StockInsuficienteException(
                        "Solo quedan " + variant.getStock() + " unidades de " + variant.getProduct().getName()
                                + " (" + variant.getColor().getName() + " / " + variant.getSize().getName() + ")");
            }
            BigDecimal unitPrice = variant.getProduct().getPromoPrice() != null
                    ? variant.getProduct().getPromoPrice()
                    : variant.getProduct().getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            detalles.add(new DetalleCalculado(variant, item.quantity(), unitPrice, subtotal));
        }

        BigDecimal subtotal = detalles.stream().map(DetalleCalculado::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingCost = resolverCostoEnvio(paymentMethod, request.district());

        Pedido pedido = new Pedido();
        pedido.setOrderNumber(sequenceService.next("PEDIDO", "PED", 8));
        pedido.setCustomer(customer);
        pedido.setPaymentMethod(paymentMethod);
        pedido.setPaymentReference(request.paymentReference());
        pedido.setSubtotal(subtotal);
        pedido.setShippingCost(shippingCost);
        pedido.setTotal(subtotal.add(shippingCost));
        pedido.setStatus(PedidoStatus.PENDING_PAYMENT);
        pedido.setRecipientDni(request.recipientDni());
        pedido.setRecipientFirstName(request.recipientFirstName());
        pedido.setRecipientLastNamePaterno(request.recipientLastNamePaterno());
        pedido.setRecipientLastNameMaterno(request.recipientLastNameMaterno());
        pedido.setPhone(request.phone());
        pedido.setAddress(request.address());
        pedido.setDepartment(request.department());
        pedido.setProvince(request.province());
        pedido.setDistrict(request.district());
        pedido.setNotes(request.notes());
        pedido.setCreatedAt(LocalDateTime.now());
        Pedido guardado = pedidoRepository.save(pedido);

        List<PedidoDetail> detallesGuardados = new ArrayList<>();
        for (DetalleCalculado dc : detalles) {
            PedidoDetail detail = new PedidoDetail();
            detail.setPedido(guardado);
            detail.setVariant(dc.variant());
            detail.setQuantity(dc.quantity());
            detail.setUnitPrice(dc.unitPrice());
            detail.setSubtotal(dc.subtotal());
            detail.setProductName(dc.variant().getProduct().getName());
            detail.setVariantSku(dc.variant().getSku());
            detail.setColorName(dc.variant().getColor().getName());
            detail.setSizeName(dc.variant().getSize().getName());
            detallesGuardados.add(pedidoDetailRepository.save(detail));
        }

        auditService.log("PEDIDO_CREADO", "PEDIDO", guardado.getId(), null,
                new Object[] { guardado.getOrderNumber(), guardado.getTotal() }, AuditResult.SUCCESS);

        return toResponse(guardado, detallesGuardados);
    }

    @Transactional
    public PedidoResponse confirmarPago(Long id, Long staffUserId) {
        Pedido pedido = buscarOFallar(id);
        if (pedido.getStatus() != PedidoStatus.PENDING_PAYMENT) {
            throw new ReglaDeNegocioException("Solo se puede confirmar un pedido pendiente de pago");
        }

        List<PedidoDetail> detalles = pedidoDetailRepository.findAllByPedidoId(id);
        for (PedidoDetail detail : detalles) {
            if (detail.getVariant().getStock() < detail.getQuantity()) {
                throw new StockInsuficienteException(
                        "Ya no hay stock suficiente de " + detail.getProductName() + " para confirmar este pedido");
            }
        }
        for (PedidoDetail detail : detalles) {
            inventarioService.registrarPorPedido(detail.getVariant().getId(), detail.getQuantity(), pedido.getId(), staffUserId);
        }

        pedido.setStatus(PedidoStatus.CONFIRMED);
        pedido.setConfirmedAt(LocalDateTime.now());
        pedido.setConfirmedBy(usuarioRepository.getReferenceById(staffUserId));

        auditService.log("PEDIDO_CONFIRMADO", "PEDIDO", pedido.getId(), null, pedido.getOrderNumber(), AuditResult.SUCCESS);
        return toResponse(pedido, detalles);
    }

    @Transactional
    public PedidoResponse cancelar(Long id, CancelarPedidoRequest request, Long staffUserId) {
        Pedido pedido = buscarOFallar(id);
        if (pedido.getStatus() == PedidoStatus.CANCELLED) {
            throw new ReglaDeNegocioException("El pedido ya está anulado");
        }

        List<PedidoDetail> detalles = pedidoDetailRepository.findAllByPedidoId(id);
        if (pedido.getStatus() == PedidoStatus.CONFIRMED) {
            for (PedidoDetail detail : detalles) {
                inventarioService.registrarPorCancelacionPedido(detail.getVariant().getId(), detail.getQuantity(), pedido.getId(), staffUserId);
            }
        }

        pedido.setStatus(PedidoStatus.CANCELLED);
        pedido.setCancelledAt(LocalDateTime.now());
        pedido.setCancellationReason(request.reason());

        auditService.log("PEDIDO_CANCELADO", "PEDIDO", pedido.getId(), null, request.reason(), AuditResult.SUCCESS);
        return toResponse(pedido, detalles);
    }

    @Transactional
    public PedidoResponse subirComprobante(Long id, Long customerId, MultipartFile file) {
        Pedido pedido = buscarOFallar(id);
        if (!pedido.getCustomer().getId().equals(customerId)) {
            throw RecursoNoEncontradoException.de("Pedido", id);
        }
        if (pedido.getStatus() != PedidoStatus.PENDING_PAYMENT) {
            throw new ReglaDeNegocioException("Solo se puede adjuntar comprobante a un pedido pendiente de pago");
        }
        pedido.setPaymentProofUrl(imageUploadService.guardar(file, "orders"));
        auditService.log("PEDIDO_COMPROBANTE_SUBIDO", "PEDIDO", pedido.getId(), null, pedido.getPaymentProofUrl(), AuditResult.SUCCESS);
        return toResponse(pedido, pedidoDetailRepository.findAllByPedidoId(id));
    }

    /**
     * Contraentrega solo se permite exactamente en el distrito de Huacho (sede física) y es
     * envío gratis (recojo/entrega local, no un envío por courier). Cualquier otro distrito
     * paga la tarifa plana configurada en company_settings — Shalom (la courier que usa la
     * empresa) no publica una API de tarifas, así que el monto lo define el staff.
     */
    private BigDecimal resolverCostoEnvio(PaymentMethod paymentMethod, String district) {
        boolean esContraentrega = CODIGO_CONTRAENTREGA.equals(paymentMethod.getCode());
        if (esContraentrega) {
            if (!DISTRITO_CONTRAENTREGA.equalsIgnoreCase(district.trim())) {
                throw new ReglaDeNegocioException(
                        "La opción de pago contraentrega solo está disponible para el distrito de Huacho");
            }
            return BigDecimal.ZERO;
        }
        return configuracionService.obtenerTarifaEnvio();
    }

    private Map<Long, ProductVariant> cargarVariantes(List<ItemPedidoRequest> items) {
        Map<Long, ProductVariant> resultado = new HashMap<>();
        for (ItemPedidoRequest item : items) {
            if (resultado.containsKey(item.variantId())) {
                continue;
            }
            ProductVariant variant = variantRepository.findById(item.variantId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Variante", item.variantId()));
            resultado.put(item.variantId(), variant);
        }
        return resultado;
    }

    private Pedido buscarOFallar(Long id) {
        return pedidoRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Pedido", id));
    }

    private PedidoResumenResponse toResumen(Pedido pedido) {
        return new PedidoResumenResponse(
                pedido.getId(), pedido.getOrderNumber(), pedido.getCustomer().getFullName(),
                pedido.getTotal(), pedido.getStatus(), pedido.getCreatedAt());
    }

    private PedidoResponse toResponse(Pedido pedido, List<PedidoDetail> detalles) {
        List<PedidoItemResponse> items = detalles.stream()
                .map(d -> new PedidoItemResponse(
                        d.getVariant().getId(), d.getProductName(), d.getVariantSku(), d.getColorName(), d.getSizeName(),
                        d.getQuantity(), d.getUnitPrice(), d.getSubtotal()))
                .toList();
        return new PedidoResponse(
                pedido.getId(),
                pedido.getOrderNumber(),
                pedido.getCustomer().getId(),
                pedido.getCustomer().getFullName(),
                pedido.getSubtotal(),
                pedido.getShippingCost(),
                pedido.getTotal(),
                pedido.getStatus(),
                pedido.getPaymentMethod().getId(),
                pedido.getPaymentMethod().getName(),
                pedido.getPaymentReference(),
                pedido.getPaymentProofUrl(),
                pedido.getRecipientDni(),
                pedido.getRecipientFirstName(),
                pedido.getRecipientLastNamePaterno(),
                pedido.getRecipientLastNameMaterno(),
                pedido.getPhone(),
                pedido.getAddress(),
                pedido.getDepartment(),
                pedido.getProvince(),
                pedido.getDistrict(),
                pedido.getNotes(),
                pedido.getCreatedAt(),
                pedido.getConfirmedAt(),
                pedido.getConfirmedBy() != null ? pedido.getConfirmedBy().getUsername() : null,
                pedido.getCancelledAt(),
                pedido.getCancellationReason(),
                items);
    }

    private record DetalleCalculado(ProductVariant variant, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
