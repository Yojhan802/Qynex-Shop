package com.freestyleperu.aplicacion.venta.service;

import com.freestyleperu.aplicacion.caja.domain.CashSession;
import com.freestyleperu.aplicacion.caja.domain.CashSessionStatus;
import com.freestyleperu.aplicacion.caja.repository.CashSessionRepository;
import com.freestyleperu.aplicacion.caja.service.CajaService;
import com.freestyleperu.aplicacion.cliente.domain.Customer;
import com.freestyleperu.aplicacion.cliente.repository.CustomerRepository;
import com.freestyleperu.aplicacion.inventario.service.InventarioService;
import com.freestyleperu.aplicacion.pago.domain.PaymentMethod;
import com.freestyleperu.aplicacion.pago.service.PagoService;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.shared.util.SequenceService;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import com.freestyleperu.aplicacion.venta.domain.Payment;
import com.freestyleperu.aplicacion.venta.domain.PaymentStatus;
import com.freestyleperu.aplicacion.venta.domain.Sale;
import com.freestyleperu.aplicacion.venta.domain.SaleDetail;
import com.freestyleperu.aplicacion.venta.domain.SaleStatus;
import com.freestyleperu.aplicacion.venta.dto.request.AnularVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.request.CrearVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.request.ItemVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.request.PagoVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.response.PagoResponse;
import com.freestyleperu.aplicacion.venta.dto.response.VentaItemResponse;
import com.freestyleperu.aplicacion.venta.dto.response.VentaResponse;
import com.freestyleperu.aplicacion.venta.dto.response.VentaResumenResponse;
import com.freestyleperu.aplicacion.venta.repository.PaymentRepository;
import com.freestyleperu.aplicacion.venta.repository.SaleDetailRepository;
import com.freestyleperu.aplicacion.venta.repository.SaleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquesta la operación más crítica del sistema: una venta completa
 * (detalle + inventario + pagos + caja + auditoría) en una única
 * transacción (docs/02-arquitectura.md §5). Si algo falla, no queda nada
 * a medias.
 */
@Service
@Transactional(readOnly = true)
public class VentaService {

    private final SaleRepository saleRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final CashSessionRepository cashSessionRepository;
    private final UsuarioRepository usuarioRepository;
    private final InventarioService inventarioService;
    private final CajaService cajaService;
    private final PagoService pagoService;
    private final SequenceService sequenceService;
    private final AuditService auditService;

    public VentaService(SaleRepository saleRepository, SaleDetailRepository saleDetailRepository,
            PaymentRepository paymentRepository, ProductVariantRepository variantRepository,
            CustomerRepository customerRepository, CashSessionRepository cashSessionRepository,
            UsuarioRepository usuarioRepository, InventarioService inventarioService, CajaService cajaService,
            PagoService pagoService, SequenceService sequenceService, AuditService auditService) {
        this.saleRepository = saleRepository;
        this.saleDetailRepository = saleDetailRepository;
        this.paymentRepository = paymentRepository;
        this.variantRepository = variantRepository;
        this.customerRepository = customerRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.usuarioRepository = usuarioRepository;
        this.inventarioService = inventarioService;
        this.cajaService = cajaService;
        this.pagoService = pagoService;
        this.sequenceService = sequenceService;
        this.auditService = auditService;
    }

    public PageResponse<VentaResumenResponse> listar(Long userId, boolean verTodas, SaleStatus status,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Long filtroUsuario = verTodas ? null : userId;
        return PageResponse.of(saleRepository.buscar(filtroUsuario, null, status, from, to, pageable), this::toResumen);
    }

    public VentaResponse obtener(Long id) {
        Sale sale = buscarOFallar(id);
        return toResponse(sale, saleDetailRepository.findAllBySaleId(id), paymentRepository.findAllBySaleId(id));
    }

    @Transactional
    public VentaResponse registrarVenta(CrearVentaRequest request, Long userId, Set<String> authorities) {
        CashSession session = cashSessionRepository.findById(request.cashSessionId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Sesión de caja", request.cashSessionId()));
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new ReglaDeNegocioException("No hay una sesión de caja abierta");
        }

        Customer customer = request.customerId() != null
                ? customerRepository.findById(request.customerId())
                        .orElseThrow(() -> RecursoNoEncontradoException.de("Cliente", request.customerId()))
                : null;

        List<ItemVentaRequest> itemsOrdenados = request.items().stream()
                .sorted(Comparator.comparing(ItemVentaRequest::variantId))
                .toList();

        boolean hayDescuento = tieneImporte(request.discountAmount())
                || itemsOrdenados.stream().anyMatch(item -> tieneImporte(item.discountAmount()));
        if (hayDescuento && !authorities.contains(Permisos.VENTAS_DESCUENTO)) {
            throw new OperacionNoPermitidaException("No tienes permisos para aplicar descuentos");
        }

        Map<Long, ProductVariant> variantesPorId = cargarVariantes(itemsOrdenados);
        List<DetalleCalculado> detalles = calcularDetalles(itemsOrdenados, variantesPorId);

        BigDecimal subtotal = detalles.stream().map(DetalleCalculado::bruto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuentoGlobal = valorOCero(request.discountAmount());
        BigDecimal descuentoLineas = detalles.stream().map(DetalleCalculado::descuento).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuentoTotal = descuentoGlobal.add(descuentoLineas);
        BigDecimal total = subtotal.subtract(descuentoTotal);
        if (total.signum() < 0) {
            throw new ReglaDeNegocioException("El descuento no puede dejar el total en negativo");
        }

        BigDecimal sumaPagos = request.payments().stream().map(PagoVentaRequest::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumaPagos.compareTo(total) != 0) {
            throw new ReglaDeNegocioException(
                    "La suma de pagos (" + sumaPagos + ") no coincide con el total (" + total + ")");
        }

        Usuario vendedor = usuarioRepository.findById(userId).orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", userId));

        Sale sale = new Sale();
        sale.setSaleNumber(sequenceService.next("VENTA", "V001", 8));
        sale.setCustomer(customer);
        sale.setUser(vendedor);
        sale.setCashSession(session);
        sale.setSubtotal(subtotal);
        sale.setDiscountAmount(descuentoTotal);
        sale.setTotal(total);
        sale.setStatus(SaleStatus.COMPLETED);
        sale.setNotes(request.notes());
        sale.setCreatedAt(LocalDateTime.now());
        Sale guardada = saleRepository.save(sale);

        List<SaleDetail> detallesGuardados = new ArrayList<>();
        for (DetalleCalculado dc : detalles) {
            SaleDetail detail = new SaleDetail();
            detail.setSale(guardada);
            detail.setVariant(dc.variant());
            detail.setQuantity(dc.quantity());
            detail.setUnitPrice(dc.unitPrice());
            detail.setDiscountAmount(dc.descuento());
            detail.setSubtotal(dc.subtotal());
            detail.setProductName(dc.variant().getProduct().getName());
            detail.setVariantSku(dc.variant().getSku());
            detail.setColorName(dc.variant().getColor().getName());
            detail.setSizeName(dc.variant().getSize().getName());
            detallesGuardados.add(saleDetailRepository.save(detail));

            inventarioService.registrarPorVenta(dc.variant().getId(), dc.quantity(), guardada.getId(), userId);
        }

        List<Payment> pagosGuardados = new ArrayList<>();
        for (PagoVentaRequest p : request.payments()) {
            PaymentMethod method = pagoService.obtenerActivoOFallar(p.paymentMethodId());
            Payment payment = new Payment();
            payment.setSale(guardada);
            payment.setPaymentMethod(method);
            payment.setAmount(p.amount());
            payment.setReference(p.reference());
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setCreatedAt(LocalDateTime.now());
            pagosGuardados.add(paymentRepository.save(payment));

            if (method.isAffectsCash()) {
                cajaService.registrarPorVenta(session.getId(), p.amount(), guardada.getId(), userId);
            }
        }

        auditService.log("VENTA_CREADA", "VENTA", guardada.getId(), null,
                new Object[] { guardada.getSaleNumber(), total }, AuditResult.SUCCESS);

        return toResponse(guardada, detallesGuardados, pagosGuardados);
    }

    @Transactional
    public VentaResponse anular(Long saleId, AnularVentaRequest request, Long userId) {
        Sale sale = buscarOFallar(saleId);
        if (sale.getStatus() == SaleStatus.CANCELLED) {
            throw new ReglaDeNegocioException("La venta ya está anulada");
        }
        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new ReglaDeNegocioException("Solo se pueden anular ventas completadas");
        }

        List<SaleDetail> detalles = saleDetailRepository.findAllBySaleId(saleId);
        for (SaleDetail detail : detalles) {
            inventarioService.registrarPorDevolucion(detail.getVariant().getId(), detail.getQuantity(), saleId, userId);
        }

        List<Payment> pagos = paymentRepository.findAllBySaleId(saleId);
        for (Payment payment : pagos) {
            if (payment.getPaymentMethod().isAffectsCash()) {
                cajaService.registrarReversion(sale.getCashSession().getId(), payment.getAmount(), saleId, userId);
            }
        }

        sale.setStatus(SaleStatus.CANCELLED);
        sale.setCancelledAt(LocalDateTime.now());
        sale.setCancelledBy(usuarioRepository.getReferenceById(userId));
        sale.setCancellationReason(request.reason());

        auditService.log("VENTA_ANULADA", "VENTA", sale.getId(), null, request.reason(), AuditResult.SUCCESS);
        return toResponse(sale, detalles, pagos);
    }

    private Map<Long, ProductVariant> cargarVariantes(List<ItemVentaRequest> items) {
        Map<Long, ProductVariant> resultado = new HashMap<>();
        for (ItemVentaRequest item : items) {
            if (resultado.containsKey(item.variantId())) {
                continue;
            }
            ProductVariant variant = variantRepository.findById(item.variantId())
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Variante", item.variantId()));
            resultado.put(item.variantId(), variant);
        }
        return resultado;
    }

    private List<DetalleCalculado> calcularDetalles(List<ItemVentaRequest> items, Map<Long, ProductVariant> variantesPorId) {
        List<DetalleCalculado> resultado = new ArrayList<>();
        for (ItemVentaRequest item : items) {
            ProductVariant variant = variantesPorId.get(item.variantId());
            BigDecimal unitPrice = variant.getProduct().getPromoPrice() != null
                    ? variant.getProduct().getPromoPrice()
                    : variant.getProduct().getPrice();
            BigDecimal cantidad = BigDecimal.valueOf(item.quantity());
            BigDecimal bruto = unitPrice.multiply(cantidad);
            BigDecimal descuento = valorOCero(item.discountAmount());
            BigDecimal subtotal = bruto.subtract(descuento);
            if (subtotal.signum() < 0) {
                throw new ReglaDeNegocioException("El descuento de " + variant.getProduct().getName() + " supera su importe");
            }
            resultado.add(new DetalleCalculado(variant, item.quantity(), unitPrice, descuento, bruto, subtotal));
        }
        return resultado;
    }

    private boolean tieneImporte(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private BigDecimal valorOCero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private Sale buscarOFallar(Long id) {
        return saleRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Venta", id));
    }

    private VentaResumenResponse toResumen(Sale sale) {
        return new VentaResumenResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getCustomer() != null ? sale.getCustomer().getFullName() : null,
                sale.getUser().getFullName(),
                sale.getTotal(),
                sale.getStatus(),
                sale.getCreatedAt());
    }

    private VentaResponse toResponse(Sale sale, List<SaleDetail> detalles, List<Payment> pagos) {
        List<VentaItemResponse> items = detalles.stream()
                .map(d -> new VentaItemResponse(
                        d.getVariant().getId(), d.getProductName(), d.getVariantSku(), d.getColorName(), d.getSizeName(),
                        d.getQuantity(), d.getUnitPrice(), d.getDiscountAmount(), d.getSubtotal()))
                .toList();
        List<PagoResponse> pagosResponse = pagos.stream()
                .map(p -> new PagoResponse(p.getPaymentMethod().getId(), p.getPaymentMethod().getName(), p.getAmount(), p.getReference()))
                .toList();
        return new VentaResponse(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getCustomer() != null ? sale.getCustomer().getId() : null,
                sale.getCustomer() != null ? sale.getCustomer().getFullName() : null,
                sale.getUser().getId(),
                sale.getUser().getFullName(),
                sale.getSubtotal(),
                sale.getDiscountAmount(),
                sale.getTotal(),
                sale.getStatus(),
                sale.getNotes(),
                sale.getCreatedAt(),
                sale.getCancelledAt(),
                sale.getCancelledBy() != null ? sale.getCancelledBy().getUsername() : null,
                sale.getCancellationReason(),
                items,
                pagosResponse);
    }

    private record DetalleCalculado(
            ProductVariant variant, int quantity, BigDecimal unitPrice, BigDecimal descuento, BigDecimal bruto, BigDecimal subtotal) {
    }
}
