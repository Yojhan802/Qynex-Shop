package com.freestyleperu.aplicacion.producto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.freestyleperu.aplicacion.catalogo.domain.Category;
import com.freestyleperu.aplicacion.catalogo.domain.Color;
import com.freestyleperu.aplicacion.catalogo.domain.Size;
import com.freestyleperu.aplicacion.catalogo.repository.CategoryRepository;
import com.freestyleperu.aplicacion.catalogo.repository.ColorRepository;
import com.freestyleperu.aplicacion.catalogo.repository.SizeRepository;
import com.freestyleperu.aplicacion.producto.dto.request.CrearProductoRequest;
import com.freestyleperu.aplicacion.producto.dto.request.CrearVarianteRequest;
import com.freestyleperu.aplicacion.producto.dto.request.GenerarVariantesRequest;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoDetalleResponse;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteBusquedaResponse;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteResponse;
import com.freestyleperu.aplicacion.producto.service.ProductoService;
import com.freestyleperu.aplicacion.producto.service.VarianteService;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductoFlujoIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ColorRepository colorRepository;
    @Autowired
    private SizeRepository sizeRepository;
    @Autowired
    private ProductoService productoService;
    @Autowired
    private VarianteService varianteService;

    @Test
    void generaSkuInternalCodeYPermiteVariantesConCodigoDeBarrasValido() {
        Category categoria = nuevaCategoria("Polos");
        Color negro = nuevoColor("Negro");
        Color blanco = nuevoColor("Blanco");
        Size talleM = nuevaTalla("M", (short) 3);
        Size talleL = nuevaTalla("L", (short) 4);

        CrearProductoRequest request = new CrearProductoRequest(
                null, null, "Polo Oversize", categoria.getId(), null, null,
                "Polo oversize de algodón", null, null, new BigDecimal("49.90"), new BigDecimal("39.90"));

        ProductoDetalleResponse producto = productoService.crear(request);

        assertThat(producto.sku()).startsWith("POL-");
        assertThat(producto.internalCode()).startsWith("POL-");
        assertThat(producto.sku()).isNotEqualTo(producto.internalCode());

        VarianteResponse variante = varianteService.crear(producto.id(),
                new CrearVarianteRequest(negro.getId(), talleM.getId(), null, null, 12, 3, true));

        assertThat(variante.sku()).isEqualTo(producto.sku() + "-M-NEG");
        assertThat(variante.barcode()).hasSize(13);
        assertThat(esEan13Valido(variante.barcode())).isTrue();

        VarianteBusquedaResponse encontrada = varianteService.buscarPorCodigoBarras(variante.barcode());
        assertThat(encontrada.effectivePrice()).isEqualByComparingTo("39.90");
        assertThat(encontrada.stock()).isEqualTo(12);

        assertThatThrownBy(() -> varianteService.crear(producto.id(),
                new CrearVarianteRequest(negro.getId(), talleM.getId(), null, null, 5, 1, false)))
                .isInstanceOf(RecursoDuplicadoException.class);

        List<VarianteResponse> generadas = varianteService.generarMatriz(producto.id(),
                new GenerarVariantesRequest(List.of(negro.getId(), blanco.getId()), List.of(talleM.getId(), talleL.getId()), 2, false));

        // Negro/M ya existía: la matriz 2x2 debe crear solo las 3 combinaciones que faltaban.
        assertThat(generadas).hasSize(3);

        List<VarianteResponse> todas = varianteService.listarPorProducto(producto.id());
        assertThat(todas).hasSize(4);

        List<VarianteResponse> segundaVez = varianteService.generarMatriz(producto.id(),
                new GenerarVariantesRequest(List.of(negro.getId(), blanco.getId()), List.of(talleM.getId(), talleL.getId()), 2, false));
        assertThat(segundaVez).isEmpty();
    }

    private boolean esEan13Valido(String codigo) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = codigo.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (codigo.charAt(12) - '0');
    }

    private Category nuevaCategoria(String nombre) {
        Category category = new Category();
        category.setName(nombre);
        category.setSlug(nombre.toLowerCase());
        return categoryRepository.save(category);
    }

    private Color nuevoColor(String nombre) {
        Color color = new Color();
        color.setName(nombre);
        color.setHexCode("#000000");
        return colorRepository.save(color);
    }

    private Size nuevaTalla(String nombre, short orden) {
        Size size = new Size();
        size.setName(nombre);
        size.setSortOrder(orden);
        return sizeRepository.save(size);
    }
}
