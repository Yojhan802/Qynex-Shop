# Prompt de diseño: plantillas de tienda Ecommerce Plus

## Objetivo

Diseñar una plantilla visual profesional para la tienda virtual multi-tenant de Qynex. La plantilla debe cambiar únicamente la presentación: no debe duplicar ni alterar la lógica de catálogo, carrito, checkout, pagos, pedidos, autenticación del cliente o notificaciones.

La plantilla se selecciona por empresa mediante un identificador seguro (`StoreTemplate`). El backend mantiene el mismo contrato para todas las plantillas.

## Plantillas oficiales

Usa uno de estos identificadores, sin inventar nombres nuevos:

- `CLASSIC`: tienda general, equilibrada y corporativa.
- `MINIMAL`: mucho espacio en blanco, tipografía limpia y foco en el producto.
- `FASHION`: moda, editorial, imágenes grandes y composición visual.
- `SPORT`: deportiva, energética, alto contraste y navegación rápida.
- `LUXURY`: premium, sobria, elegante y con pocos elementos visuales.
- `BOUTIQUE`: cálida, cercana y orientada a marcas pequeñas.
- `CATALOG`: catálogo denso, filtros visibles y comparación rápida.
- `MARKET`: variedad de categorías y navegación por secciones.
- `EDITORIAL`: portada narrativa, banners y bloques de contenido.
- `URBAN`: moderna, juvenil y optimizada para móvil.

## Páginas que debe cubrir

La propuesta debe funcionar en todas estas páginas:

1. Inicio y catálogo (`tienda/index.html`).
2. Detalle de producto (`tienda/producto.html`).
3. Carrito (`tienda/carrito.html`).
4. Checkout (`tienda/checkout.html`).
5. Inicio de sesión del cliente (`tienda/cuenta/login.html`).
6. Registro del cliente (`tienda/cuenta/registro.html`).
7. Mis pedidos (`tienda/cuenta/pedidos.html`).
8. Tienda no disponible o suspendida (`tienda/no-disponible.html`).

## Datos dinámicos obligatorios

La plantilla debe soportar, sin datos ficticios:

- Nombre y logo de la empresa.
- Categorías y marcas.
- Productos, imágenes, precios, descuentos y disponibilidad.
- Variantes, atributos, colores y tallas.
- Carrito persistido.
- Métodos de pago y sus instrucciones.
- Tarifa y reglas de envío.
- Estados del pedido y comprobante de pago.
- Sesión, registro y pedidos del cliente.
- Mensajes de error, carga, vacío y tienda suspendida.

No codifiques productos, precios, nombres de empresas, medios de pago ni URLs de API dentro de la plantilla.

## Reglas técnicas

- Reutilizar las APIs y componentes existentes de `front/tienda/js/store`.
- No modificar las reglas de stock, precios, promociones, pagos o pedidos.
- No duplicar el checkout ni crear un segundo carrito.
- No guardar secretos, tokens ni credenciales en el navegador.
- No insertar HTML o JavaScript proporcionado libremente por el cliente.
- Usar únicamente el identificador de plantilla permitido por el backend.
- Escapar todo texto dinámico antes de insertarlo en HTML.
- Mantener soporte para subdominios y resolución multi-tenant.
- No depender de servicios externos obligatorios para que la tienda cargue.
- Mantener tiempos de carga bajos y optimizar imágenes responsivas.

## Requisitos visuales

- Diseño responsive desde 320 px hasta escritorio amplio.
- Mobile-first: navegación, filtros, carrito y checkout deben ser cómodos con una mano.
- Contraste WCAG AA como mínimo.
- Foco visible y navegación completa por teclado.
- Botones táctiles de al menos 44 px.
- Estados hover, focus, disabled, loading, error, vacío y sin stock.
- Jerarquía visual clara para precio, descuento, disponibilidad y llamada a la acción.
- Logo y colores de la empresa deben integrarse sin romper el contraste.
- No usar animaciones que bloqueen la compra; respetar `prefers-reduced-motion`.

## Personalización permitida

La plantilla puede exponer variables visuales configurables:

- Color principal y color de acento.
- Fondo claro u oscuro.
- Tipografía de una lista aprobada.
- Forma de tarjetas y botones.
- Logo y favicon.
- Texto de bienvenida.
- Banner principal e imágenes promocionales.
- Visibilidad de categorías, marcas, promociones y asistente IA.

No permitas CSS arbitrario ni JavaScript arbitrario desde la configuración del cliente.

## Entregables por plantilla

Entrega lo siguiente:

1. Nombre comercial y finalidad de la plantilla.
2. Referencia visual de escritorio y móvil.
3. Paleta, tipografías, espaciado, bordes y sombras.
4. Estructura de inicio, catálogo, producto, carrito y checkout.
5. Estados vacíos, error, carga, sin stock y tienda suspendida.
6. Lista de componentes reutilizables.
7. Tokens CSS y variables configurables.
8. Relación de archivos creados o modificados.
9. Evidencia responsive en 320 px, 768 px y 1440 px.
10. Checklist de accesibilidad y regresión funcional.

## Criterios de aceptación

La plantilla se considera lista cuando:

- La empresa puede seleccionarla desde su panel.
- La tienda pública la carga según su tenant.
- El catálogo real sigue funcionando.
- El carrito conserva sus productos al navegar.
- El checkout crea pedidos correctamente.
- Los pagos manuales y online conservan su flujo.
- El cliente puede registrarse, iniciar sesión y consultar pedidos.
- No se filtran datos de otra empresa.
- La plantilla clásica sigue funcionando como fallback.
- `node --check` y las pruebas del backend pasan.
- Un cambio visual no modifica el comportamiento comercial.

## Instrucción para el agente de diseño

Antes de crear archivos, inspecciona la estructura existente de `front/tienda`, `store-shell.js`, `store-api.js`, las páginas del checkout y los contratos de las APIs. Propón primero la composición y los tokens. Después implementa la plantilla respetando este contrato y verifica todos los estados. Si una decisión visual requiere cambiar la lógica de negocio, detente y repórtala en vez de modificarla.
