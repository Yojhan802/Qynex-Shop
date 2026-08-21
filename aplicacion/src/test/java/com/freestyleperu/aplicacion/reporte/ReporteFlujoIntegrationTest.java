package com.freestyleperu.aplicacion.reporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.freestyleperu.aplicacion.caja.domain.CashRegister;
import com.freestyleperu.aplicacion.caja.dto.request.AbrirCajaRequest;
import com.freestyleperu.aplicacion.caja.dto.response.SesionCajaResponse;
import com.freestyleperu.aplicacion.caja.repository.CashRegisterRepository;
import com.freestyleperu.aplicacion.caja.service.CajaService;
import com.freestyleperu.aplicacion.catalogo.domain.Category;
import com.freestyleperu.aplicacion.catalogo.domain.Color;
import com.freestyleperu.aplicacion.catalogo.domain.Size;
import com.freestyleperu.aplicacion.catalogo.repository.CategoryRepository;
import com.freestyleperu.aplicacion.catalogo.repository.ColorRepository;
import com.freestyleperu.aplicacion.catalogo.repository.SizeRepository;
import com.freestyleperu.aplicacion.inventario.domain.Branch;
import com.freestyleperu.aplicacion.inventario.domain.Warehouse;
import com.freestyleperu.aplicacion.inventario.repository.BranchRepository;
import com.freestyleperu.aplicacion.inventario.repository.WarehouseRepository;
import com.freestyleperu.aplicacion.pago.domain.PaymentMethod;
import com.freestyleperu.aplicacion.pago.domain.PaymentMethodType;
import com.freestyleperu.aplicacion.pago.repository.PaymentMethodRepository;
import com.freestyleperu.aplicacion.producto.dto.request.CrearProductoRequest;
import com.freestyleperu.aplicacion.producto.dto.request.CrearVarianteRequest;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoDetalleResponse;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteResponse;
import com.freestyleperu.aplicacion.producto.service.ProductoService;
import com.freestyleperu.aplicacion.producto.service.VarianteService;
import com.freestyleperu.aplicacion.reporte.dto.response.DashboardResponse;
import com.freestyleperu.aplicacion.reporte.service.ReporteService;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import com.freestyleperu.aplicacion.venta.dto.request.CrearVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.request.ItemVentaRequest;
import com.freestyleperu.aplicacion.venta.dto.request.PagoVentaRequest;
import com.freestyleperu.aplicacion.venta.service.VentaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReporteFlujoIntegrationTest {

    @Autowired private BranchRepository branchRepository;
    @Autowired private WarehouseRepository warehouseRepository;
    @Autowired private CashRegisterRepository cashRegisterRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private PaymentMethodRepository paymentMethodRepository;
    @Autowired private ProductoService productoService;
    @Autowired private VarianteService varianteService;
    @Autowired private CajaService cajaService;
    @Autowired private VentaService ventaService;
    @Autowired private ReporteService reporteService;

    @Test
    void elDashboardReflejaUnaVentaRecienRegistradaHoyYEsteMes() {
        Branch branch = new Branch();
        branch.setCode("SUC-REP-TEST");
        branch.setName("Sucursal reporte test");
        branchRepository.save(branch);

        Warehouse warehouse = new Warehouse();
        warehouse.setBranch(branch);
        warehouse.setCode("ALM-REP-TEST");
        warehouse.setName("Almacén reporte test");
        warehouseRepository.save(warehouse);

        CashRegister register = new CashRegister();
        register.setBranch(branch);
        register.setCode("CAJA-REP-TEST");
        register.setName("Caja reporte test");
        cashRegisterRepository.save(register);

        Rol rol = new Rol();
        rol.setCode("TEST_ROL_REPORTE");
        rol.setName("Rol de prueba reporte");
        rol.setSystem(false);
        rolRepository.save(rol);

        Usuario usuario = new Usuario();
        usuario.setUsername("vendedor.reporte");
        usuario.setPasswordHash("hash");
        usuario.setFullName("Vendedor Reporte");
        usuario.setStatus(UsuarioEstado.ACTIVE);
        usuario.setRoles(new HashSet<>(List.of(rol)));
        usuarioRepository.save(usuario);
        Long userId = usuario.getId();

        SesionCajaResponse sesion = cajaService.abrirCaja(new AbrirCajaRequest(register.getId(), new BigDecimal("200.00")), userId);

        Category categoria = new Category();
        categoria.setName("Polos-reporte");
        categoria.setSlug("polos-reporte");
        categoryRepository.save(categoria);

        Color color = new Color();
        color.setName("Negro-reporte");
        color.setHexCode("#000000");
        colorRepository.save(color);

        Size talla = new Size();
        talla.setName("M-reporte");
        talla.setSortOrder((short) 1);
        sizeRepository.save(talla);

        ProductoDetalleResponse producto = productoService.crear(new CrearProductoRequest(
                null, null, "Polo Reporte", categoria.getId(), null, null, null, null, null, new BigDecimal("50.00"), null));
        VarianteResponse variante = varianteService.crear(producto.id(),
                new CrearVarianteRequest(color.getId(), talla.getId(), null, null, 20, 1, false));

        PaymentMethod efectivo = new PaymentMethod();
        efectivo.setCode("EFECTIVO-REP");
        efectivo.setName("Efectivo");
        efectivo.setType(PaymentMethodType.CASH);
        efectivo.setAffectsCash(true);
        efectivo.setRequiresReference(false);
        efectivo.setSortOrder((short) 1);
        paymentMethodRepository.save(efectivo);

        ventaService.registrarVenta(new CrearVentaRequest(
                        null, null, sesion.id(), null, null,
                        List.of(new ItemVentaRequest(variante.id(), 3, null)),
                        List.of(new PagoVentaRequest(efectivo.getId(), new BigDecimal("150.00"), null))),
                userId, Set.of());

        DashboardResponse dashboard = reporteService.dashboard();

        assertThat(dashboard.salesToday().count()).isGreaterThanOrEqualTo(1);
        assertThat(dashboard.salesToday().total()).isGreaterThanOrEqualTo(new BigDecimal("150.00"));
        assertThat(dashboard.salesMonth().total()).isGreaterThanOrEqualTo(new BigDecimal("150.00"));
        assertThat(dashboard.productsSoldToday()).isGreaterThanOrEqualTo(3);
        assertThat(dashboard.salesByDay()).hasSize(7);
        assertThat(dashboard.salesByDay().get(6).date()).isEqualTo(LocalDate.now());
        assertThat(dashboard.salesByDay().get(6).total()).isGreaterThanOrEqualTo(new BigDecimal("150.00"));
        assertThat(dashboard.paymentBreakdown()).isNotEmpty();
        assertThat(dashboard.topProducts()).isNotEmpty();

        var porDia = reporteService.ventasPorDia(LocalDate.now().minusDays(6), LocalDate.now());
        assertThat(porDia).hasSize(7);

        var porCategoria = reporteService.ventasPorCategoria(LocalDate.now(), LocalDate.now());
        assertThat(porCategoria).anyMatch(s -> s.label().equals("Polos-reporte"));

        var porVendedor = reporteService.ventasPorVendedor(LocalDate.now(), LocalDate.now());
        assertThat(porVendedor).anyMatch(s -> s.label().equals("Vendedor Reporte"));
    }
}
