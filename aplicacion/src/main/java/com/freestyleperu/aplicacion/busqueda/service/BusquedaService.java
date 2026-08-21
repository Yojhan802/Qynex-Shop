package com.freestyleperu.aplicacion.busqueda.service;

import com.freestyleperu.aplicacion.busqueda.dto.response.SearchResponse;
import com.freestyleperu.aplicacion.busqueda.dto.response.SearchResultItem;
import com.freestyleperu.aplicacion.cliente.domain.Customer;
import com.freestyleperu.aplicacion.cliente.repository.CustomerRepository;
import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.repository.ProductRepository;
import com.freestyleperu.aplicacion.producto.repository.ProductVariantRepository;
import com.freestyleperu.aplicacion.shared.domain.EstadoGeneral;
import com.freestyleperu.aplicacion.shared.security.AuthenticatedUser;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import com.freestyleperu.aplicacion.venta.domain.Sale;
import com.freestyleperu.aplicacion.venta.repository.SaleRepository;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Búsqueda global (docs §17, documento.txt §50) — cada categoría de resultado
 * solo se incluye si el usuario tiene el permiso de consulta correspondiente
 * (RN-20: autorización por permiso, no por rol), en vez de bloquear todo el
 * endpoint por un único permiso.
 */
@Service
@Transactional(readOnly = true)
public class BusquedaService {

    private static final int LIMITE_POR_CATEGORIA = 6;

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final UsuarioRepository usuarioRepository;

    public BusquedaService(ProductRepository productRepository, ProductVariantRepository variantRepository,
            CustomerRepository customerRepository, SaleRepository saleRepository, UsuarioRepository usuarioRepository) {
        this.productRepository = productRepository;
        this.variantRepository = variantRepository;
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public SearchResponse buscar(String query, AuthenticatedUser currentUser) {
        String q = query.trim();
        return new SearchResponse(
                currentUser.tienePermiso(Permisos.PRODUCTOS_CONSULTAR) ? buscarProductos(q) : List.of(),
                currentUser.tienePermiso(Permisos.CLIENTES_CONSULTAR) ? buscarClientes(q) : List.of(),
                currentUser.tienePermiso(Permisos.VENTAS_CONSULTAR) ? buscarVentas(q) : List.of(),
                currentUser.tienePermiso(Permisos.USUARIOS_CONSULTAR) ? buscarUsuarios(q) : List.of());
    }

    /** Busca por nombre/SKU/código interno de producto y por SKU/código de barras de variante, unificado por producto. */
    private List<SearchResultItem> buscarProductos(String q) {
        // Map en vez de Set: conserva el orden de primer hallazgo y deduplica por id de producto.
        LinkedHashMap<Long, Product> encontrados = new LinkedHashMap<>();
        productRepository.buscar(q, null, null, null, EstadoGeneral.ACTIVE, null, null,
                        PageRequest.of(0, LIMITE_POR_CATEGORIA))
                .forEach(p -> encontrados.put(p.getId(), p));
        variantRepository.buscar(q).stream()
                .map(v -> v.getProduct())
                .filter(p -> p.getStatus() == EstadoGeneral.ACTIVE)
                .forEach(p -> encontrados.putIfAbsent(p.getId(), p));

        return encontrados.values().stream()
                .limit(LIMITE_POR_CATEGORIA)
                .map(p -> new SearchResultItem("PRODUCTO", p.getId(), p.getName(), p.getSku(),
                        "producto-detalle.html?id=" + p.getId()))
                .toList();
    }

    private List<SearchResultItem> buscarClientes(String q) {
        return customerRepository.buscarRapido(q).stream()
                .limit(LIMITE_POR_CATEGORIA)
                .map(this::toClienteItem)
                .toList();
    }

    private SearchResultItem toClienteItem(Customer c) {
        String subtitulo = c.getDocNumber() != null ? c.getDocNumber() : c.getPhone();
        return new SearchResultItem("CLIENTE", c.getId(), c.getFullName(), subtitulo, "clientes.html");
    }

    private List<SearchResultItem> buscarVentas(String q) {
        return saleRepository.buscarPorNumero(q, PageRequest.of(0, LIMITE_POR_CATEGORIA)).stream()
                .map(this::toVentaItem)
                .toList();
    }

    private SearchResultItem toVentaItem(Sale s) {
        String subtitulo = s.getCustomer() != null ? s.getCustomer().getFullName() : s.getUser().getFullName();
        return new SearchResultItem("VENTA", s.getId(), s.getSaleNumber(), subtitulo, "pos.html?ventaId=" + s.getId());
    }

    private List<SearchResultItem> buscarUsuarios(String q) {
        return usuarioRepository.buscar(q, null, PageRequest.of(0, LIMITE_POR_CATEGORIA)).stream()
                .map(this::toUsuarioItem)
                .toList();
    }

    private SearchResultItem toUsuarioItem(Usuario u) {
        return new SearchResultItem("USUARIO", u.getId(), u.getFullName(), u.getUsername(), "usuarios.html");
    }
}
