package com.freestyleperu.aplicacion.ia;

import com.freestyleperu.aplicacion.configuracion.service.ConfiguracionService;
import com.freestyleperu.aplicacion.ia.dto.AsistenteHistorialItem;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicCategoriaResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicMetodoPagoResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicProductoDetalleResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicProductoResumenResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicShippingInfoResponse;
import com.freestyleperu.aplicacion.tienda.dto.response.PublicVarianteResponse;
import com.freestyleperu.aplicacion.tienda.service.TiendaCatalogoService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asistente de compras de la tienda pública (plan IA). El system prompt solo
 * incluye datos reales leídos en el momento (catálogo con tallas/colores/stock
 * reales, envío, métodos de pago) — nunca se le pide al modelo que invente
 * algo que no esté en ese contexto. El historial lo manda el frontend (sin
 * sesión en el backend) para que la conversación tenga memoria de un turno a otro.
 */
@Service
@Transactional(readOnly = true)
public class AsistenteTiendaService {

    private static final int MAX_PRODUCTOS_CONTEXTO = 6;
    private static final int MIN_LARGO_PALABRA_NORMALIZABLE = 4;
    private static final int MAX_TURNOS_HISTORIAL = 8;

    private final TiendaCatalogoService catalogoService;
    private final ConfiguracionService configuracionService;
    private final OpenRouterClient openRouterClient;

    public AsistenteTiendaService(TiendaCatalogoService catalogoService, ConfiguracionService configuracionService,
            OpenRouterClient openRouterClient) {
        this.catalogoService = catalogoService;
        this.configuracionService = configuracionService;
        this.openRouterClient = openRouterClient;
    }

    public String responder(String mensajeCliente, List<AsistenteHistorialItem> historial) {
        List<AsistenteHistorialItem> turnosRecientes = ultimosTurnos(historial);
        String systemPrompt = construirSystemPrompt(mensajeCliente, turnosRecientes);

        List<OpenRouterClient.OpenRouterMessage> mensajes = new ArrayList<>();
        mensajes.add(new OpenRouterClient.OpenRouterMessage("system", systemPrompt));
        turnosRecientes.forEach(h -> mensajes.add(new OpenRouterClient.OpenRouterMessage(h.role(), h.content())));
        mensajes.add(new OpenRouterClient.OpenRouterMessage("user", mensajeCliente));

        return openRouterClient.completar(mensajes);
    }

    /** El frontend puede mandar una conversación larga — nos quedamos solo con los últimos turnos para no disparar el costo. */
    private List<AsistenteHistorialItem> ultimosTurnos(List<AsistenteHistorialItem> historial) {
        int desde = Math.max(0, historial.size() - MAX_TURNOS_HISTORIAL);
        return historial.subList(desde, historial.size());
    }

    private String construirSystemPrompt(String mensajeCliente, List<AsistenteHistorialItem> historial) {
        String nombreTienda = configuracionService.obtenerBranding().name();
        PublicShippingInfoResponse envio = catalogoService.obtenerInfoEnvio();
        List<PublicMetodoPagoResponse> metodosPago = catalogoService.listarMetodosPago();
        List<PublicProductoResumenResponse> productos = buscarProductosRelevantes(mensajeCliente, historial);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres el asistente virtual de la tienda online de ").append(nombreTienda)
                .append(", un negocio de ropa en Perú. Responde siempre en español, de forma breve, cordial y directa.\n\n");
        prompt.append("SOLO puedes hablar de esta tienda: sus productos, envíos, métodos de pago y el proceso de compra. ")
                .append("Si te preguntan algo fuera de eso (aunque sepas la respuesta), dilo amablemente sin responderla y redirige la conversación a la tienda.\n\n");
        prompt.append("Nunca inventes stock, tallas, colores o precios que no estén en la información de abajo. ")
                .append("Si preguntan por una talla o color específico, revisa la lista de cada producto antes de responder. ")
                .append("Si no tienes el dato, dile al cliente que lo confirme en la ficha del producto o al finalizar su pedido. ")
                .append("Si preguntan por tallas/colores/precio sin decir de qué producto (y no hay ninguno en la lista de abajo ni se mencionó antes en la conversación), ")
                .append("pregunta amablemente a cuál producto se refiere en vez de negarte a ayudar.\n\n");
        prompt.append("Cuando hables de un producto puntual, incluye siempre su enlace exactamente en este formato, ")
                .append("como texto plano y sin nada alrededor (sin https://, sin dominio, sin corchetes): ")
                .append("producto.html?id=<ID>. Es la única forma que tiene el cliente de \"ir al producto\".\n\n");

        prompt.append("Envío: S/ ").append(envio.flatRate()).append(" a todo el Perú, gratis en ")
                .append(envio.freeShippingDistrict()).append(".\n");

        prompt.append("Métodos de pago disponibles: ").append(
                metodosPago.stream().map(PublicMetodoPagoResponse::name).collect(Collectors.joining(", ")))
                .append(".\n\n");

        if (productos.isEmpty()) {
            prompt.append("No se encontraron productos del catálogo relacionados con el mensaje del cliente.\n");
        } else {
            prompt.append("Productos del catálogo relevantes para su pregunta, con tallas/colores y disponibilidad reales:\n");
            productos.forEach(p -> {
                PublicProductoDetalleResponse detalle = catalogoService.obtenerProducto(p.id());
                prompt.append("- ").append(p.name())
                        .append(" (id ").append(p.id()).append(", ").append(p.categoryName()).append(")")
                        .append(", S/ ").append(p.promoPrice() != null ? p.promoPrice() : p.price())
                        .append(" — tallas: ").append(tallasDisponibles(detalle))
                        .append(" — colores: ").append(coloresDisponibles(detalle))
                        .append(" — enlace: producto.html?id=").append(p.id())
                        .append("\n");
            });
        }

        return prompt.toString();
    }

    /**
     * Búsqueda de texto libre por LIKE (ver ProductRepository) no reconoce
     * "camisas en talla s" como relacionado a la categoría "Camisas" — así
     * que primero intentamos resolver una categoría o marca mencionada en el
     * mensaje (o en el historial reciente — "llévame al producto" no dice
     * nada por sí solo, pero si dos turnos antes se habló de "Camisa de
     * Vestir", seguimos entendiendo de qué producto se trata sin depender de
     * que el modelo haya repetido el ID correctamente) y filtramos por eso;
     * solo si no hay match usamos la búsqueda de texto plano del mensaje
     * actual (útil para "tienen la casaca denim?", que sí es un nombre).
     */
    private List<PublicProductoResumenResponse> buscarProductosRelevantes(String mensajeCliente, List<AsistenteHistorialItem> historial) {
        String contextoReciente = historial.stream().map(AsistenteHistorialItem::content).collect(Collectors.joining(" "))
                + " " + mensajeCliente;
        List<String> palabrasContexto = normalizarMensaje(contextoReciente);

        Long categoriaId = catalogoService.listarCategorias().stream()
                .filter(c -> palabrasContexto.contains(normalizarPalabra(c.name())))
                .map(PublicCategoriaResponse::id)
                .findFirst().orElse(null);

        Long marcaId = catalogoService.listarMarcas().stream()
                .filter(m -> palabrasContexto.contains(normalizarPalabra(m.name())))
                .map(m -> m.id())
                .findFirst().orElse(null);

        if (categoriaId != null || marcaId != null) {
            return catalogoService
                    .listarProductos(null, categoriaId, marcaId, PageRequest.of(0, MAX_PRODUCTOS_CONTEXTO))
                    .content();
        }
        return catalogoService
                .listarProductos(mensajeCliente, null, null, PageRequest.of(0, MAX_PRODUCTOS_CONTEXTO))
                .content();
    }

    private List<String> normalizarMensaje(String mensaje) {
        return Arrays.stream(mensaje.toLowerCase().split("\\W+"))
                .map(this::normalizarPalabra)
                .collect(Collectors.toList());
    }

    /** Quita el plural español más común (-es/-s) para poder comparar "camisas" con "Camisa". */
    private String normalizarPalabra(String palabra) {
        String p = palabra.toLowerCase();
        if (p.length() <= MIN_LARGO_PALABRA_NORMALIZABLE) return p;
        if (p.endsWith("es")) return p.substring(0, p.length() - 2);
        if (p.endsWith("s")) return p.substring(0, p.length() - 1);
        return p;
    }

    /** Agrupa variantes por talla: disponible si al menos un color de esa talla tiene stock. */
    private String tallasDisponibles(PublicProductoDetalleResponse detalle) {
        Map<String, Boolean> porTalla = new LinkedHashMap<>();
        for (PublicVarianteResponse v : detalle.variants()) {
            porTalla.merge(v.sizeName(), v.inStock(), (a, b) -> a || b);
        }
        if (porTalla.isEmpty()) return "sin información de tallas";
        return porTalla.entrySet().stream()
                .map(e -> e.getKey() + (e.getValue() ? " (disponible)" : " (agotada)"))
                .collect(Collectors.joining(", "));
    }

    /** Agrupa variantes por color: disponible si al menos una talla de ese color tiene stock. */
    private String coloresDisponibles(PublicProductoDetalleResponse detalle) {
        Map<String, Boolean> porColor = new LinkedHashMap<>();
        for (PublicVarianteResponse v : detalle.variants()) {
            porColor.merge(v.colorName(), v.inStock(), (a, b) -> a || b);
        }
        if (porColor.isEmpty()) return "sin información de colores";
        return porColor.entrySet().stream()
                .map(e -> e.getKey() + (e.getValue() ? " (disponible)" : " (agotado)"))
                .collect(Collectors.joining(", "));
    }
}
