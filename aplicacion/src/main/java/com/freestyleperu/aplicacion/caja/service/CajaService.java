package com.freestyleperu.aplicacion.caja.service;

import com.freestyleperu.aplicacion.caja.domain.CashMovement;
import com.freestyleperu.aplicacion.caja.domain.CashMovementType;
import com.freestyleperu.aplicacion.caja.domain.CashReferenceType;
import com.freestyleperu.aplicacion.caja.domain.CashRegister;
import com.freestyleperu.aplicacion.caja.domain.CashSession;
import com.freestyleperu.aplicacion.caja.domain.CashSessionStatus;
import com.freestyleperu.aplicacion.caja.dto.request.AbrirCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.request.CerrarCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.request.MovimientoCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.response.CajaResponse;
import com.freestyleperu.aplicacion.caja.dto.response.MovimientoCajaResponse;
import com.freestyleperu.aplicacion.caja.dto.response.ResumenCierreResponse;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaDetalleResponse;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaResponse;
import com.freestyleperu.aplicacion.caja.repository.CashMovementRepository;
import com.freestyleperu.aplicacion.caja.repository.CashRegisterRepository;
import com.freestyleperu.aplicacion.caja.repository.CashSessionRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.dto.PageResponse;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CajaService {

    private static final Set<CashMovementType> TIPOS_MANUALES = Set.of(
            CashMovementType.INGRESO, CashMovementType.GASTO, CashMovementType.RETIRO);

    private final CashRegisterRepository cashRegisterRepository;
    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public CajaService(CashRegisterRepository cashRegisterRepository, CashSessionRepository cashSessionRepository,
            CashMovementRepository cashMovementRepository, UsuarioRepository usuarioRepository, AuditService auditService) {
        this.cashRegisterRepository = cashRegisterRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.cashMovementRepository = cashMovementRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    public List<CajaResponse> listarCajas() {
        return cashRegisterRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public PageResponse<SesionCajaResponse> listarSesiones(Long cashRegisterId, CashSessionStatus status, Pageable pageable) {
        return PageResponse.of(cashSessionRepository.buscar(cashRegisterId, status, pageable), this::toResponse);
    }

    public SesionCajaDetalleResponse obtenerSesion(Long id) {
        return toDetalle(buscarSesionOFallar(id));
    }

    public SesionCajaDetalleResponse obtenerSesionActual(Long userId) {
        CashSession session = cashSessionRepository.findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(userId, CashSessionStatus.OPEN)
                .orElseThrow(() -> new RecursoNoEncontradoException("No tienes ninguna sesión de caja abierta"));
        return toDetalle(session);
    }

    public ResumenCierreResponse obtenerResumenCierre(Long id) {
        CashSession session = buscarSesionOFallar(id);
        BigDecimal movementsTotal = cashMovementRepository.sumaPorSesion(id);
        return new ResumenCierreResponse(session.getOpeningAmount(), movementsTotal, session.getOpeningAmount().add(movementsTotal));
    }

    @Transactional
    public SesionCajaResponse abrirCaja(AbrirCajaRequest request, Long userId) {
        CashRegister register = cashRegisterRepository.findById(request.cashRegisterId())
                .orElseThrow(() -> RecursoNoEncontradoException.de("Caja", request.cashRegisterId()));

        if (cashSessionRepository.existsByCashRegisterIdAndStatus(register.getId(), CashSessionStatus.OPEN)) {
            throw new ReglaDeNegocioException("La caja " + register.getName() + " ya tiene una sesión abierta");
        }

        CashSession session = new CashSession();
        session.setCashRegister(register);
        session.setOpenedBy(usuarioRepository.getReferenceById(userId));
        session.setOpeningAmount(request.openingAmount());
        session.setOpenedAt(LocalDateTime.now());
        session.setStatus(CashSessionStatus.OPEN);

        CashSession guardada = cashSessionRepository.save(session);
        auditService.log("CAJA_ABIERTA", "SESION_CAJA", guardada.getId(), null, request.openingAmount(), AuditResult.SUCCESS);
        return toResponse(guardada);
    }

    @Transactional
    public SesionCajaResponse cerrarCaja(Long id, CerrarCajaRequest request, Long userId) {
        CashSession session = buscarSesionOFallar(id);
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new ReglaDeNegocioException("La sesión de caja ya está cerrada");
        }

        BigDecimal movementsTotal = cashMovementRepository.sumaPorSesion(id);
        BigDecimal expected = session.getOpeningAmount().add(movementsTotal);
        BigDecimal difference = request.countedAmount().subtract(expected);

        session.setExpectedAmount(expected);
        session.setCountedAmount(request.countedAmount());
        session.setDifference(difference);
        session.setClosedBy(usuarioRepository.getReferenceById(userId));
        session.setClosedAt(LocalDateTime.now());
        session.setStatus(CashSessionStatus.CLOSED);
        session.setNotes(request.notes());

        auditService.log("CAJA_CERRADA", "SESION_CAJA", session.getId(), expected,
                new Object[] { request.countedAmount(), difference }, AuditResult.SUCCESS);
        return toResponse(session);
    }

    @Transactional
    public MovimientoCajaResponse registrarMovimiento(MovimientoCajaRequest request, Long userId) {
        if (!TIPOS_MANUALES.contains(request.type())) {
            throw new OperacionNoPermitidaException("Este movimiento solo puede registrarse automáticamente por una venta o devolución");
        }
        CashSession session = cashSessionRepository.findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(userId, CashSessionStatus.OPEN)
                .orElseThrow(() -> new ReglaDeNegocioException("No tienes ninguna sesión de caja abierta"));

        BigDecimal amount = request.type() == CashMovementType.INGRESO ? request.amount() : request.amount().negate();

        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(request.type());
        movement.setAmount(amount);
        movement.setReason(request.reason());
        movement.setUser(usuarioRepository.getReferenceById(userId));
        movement.setCreatedAt(LocalDateTime.now());

        CashMovement guardado = cashMovementRepository.save(movement);
        auditService.log("CAJA_MOVIMIENTO", "SESION_CAJA", session.getId(), null, amount, AuditResult.SUCCESS);
        return toResponse(guardado);
    }

    /** Invocado por {@code VentaService}, dentro de su propia transacción, al cobrar en efectivo. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPorVenta(Long cashSessionId, BigDecimal amount, Long saleId, Long userId) {
        registrarInterno(cashSessionId, CashMovementType.VENTA, amount, CashReferenceType.SALE, saleId, userId);
    }

    /**
     * Revierte el efectivo de una venta anulada o devuelta. Si la sesión original
     * ya está cerrada, se registra contra la sesión abierta actual del usuario.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarReversion(Long cashSessionOriginalId, BigDecimal amount, Long saleId, Long userId) {
        CashSession original = buscarSesionOFallar(cashSessionOriginalId);
        Long sessionDestinoId = original.getStatus() == CashSessionStatus.OPEN
                ? cashSessionOriginalId
                : cashSessionRepository.findFirstByOpenedByIdAndStatusOrderByOpenedAtDesc(userId, CashSessionStatus.OPEN)
                        .orElseThrow(() -> new ReglaDeNegocioException(
                                "La sesión original ya está cerrada y no tienes ninguna sesión de caja abierta para registrar la reversión"))
                        .getId();
        registrarInterno(sessionDestinoId, CashMovementType.DEVOLUCION, amount.negate(), CashReferenceType.RETURN, saleId, userId);
    }

    private void registrarInterno(Long cashSessionId, CashMovementType type, BigDecimal amount,
            CashReferenceType referenceType, Long referenceId, Long userId) {
        CashSession session = buscarSesionOFallar(cashSessionId);
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new ReglaDeNegocioException("La sesión de caja está cerrada");
        }
        CashMovement movement = new CashMovement();
        movement.setCashSession(session);
        movement.setType(type);
        movement.setAmount(amount);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        movement.setUser(usuarioRepository.getReferenceById(userId));
        movement.setCreatedAt(LocalDateTime.now());
        cashMovementRepository.save(movement);
    }

    private CashSession buscarSesionOFallar(Long id) {
        return cashSessionRepository.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Sesión de caja", id));
    }

    private CajaResponse toResponse(CashRegister register) {
        return new CajaResponse(register.getId(), register.getBranch().getName(), register.getCode(), register.getName(), register.getStatus());
    }

    private SesionCajaResponse toResponse(CashSession session) {
        return new SesionCajaResponse(
                session.getId(),
                session.getCashRegister().getId(),
                session.getCashRegister().getName(),
                session.getOpenedBy().getUsername(),
                session.getOpeningAmount(),
                session.getOpenedAt(),
                session.getExpectedAmount(),
                session.getCountedAmount(),
                session.getDifference(),
                session.getClosedBy() != null ? session.getClosedBy().getUsername() : null,
                session.getClosedAt(),
                session.getStatus(),
                session.getNotes());
    }

    private SesionCajaDetalleResponse toDetalle(CashSession session) {
        List<MovimientoCajaResponse> movimientos = cashMovementRepository
                .findAllByCashSessionIdOrderByCreatedAtDesc(session.getId(), Pageable.unpaged())
                .map(this::toResponse)
                .getContent();
        return new SesionCajaDetalleResponse(
                session.getId(),
                session.getCashRegister().getId(),
                session.getCashRegister().getName(),
                session.getOpenedBy().getUsername(),
                session.getOpeningAmount(),
                session.getOpenedAt(),
                session.getExpectedAmount(),
                session.getCountedAmount(),
                session.getDifference(),
                session.getClosedBy() != null ? session.getClosedBy().getUsername() : null,
                session.getClosedAt(),
                session.getStatus(),
                session.getNotes(),
                movimientos);
    }

    private MovimientoCajaResponse toResponse(CashMovement movement) {
        return new MovimientoCajaResponse(
                movement.getId(),
                movement.getCashSession().getId(),
                movement.getType(),
                movement.getAmount(),
                movement.getReason(),
                movement.getUser().getUsername(),
                movement.getCreatedAt());
    }
}
