# 06 — Identidad visual y Design System

Derivado del análisis del material de marca en [recursos/](../recursos/).

---

## 1. Análisis del logo

**Archivo:** `recursos/image.png` (2025 × 2026 px, PNG)

**Qué es:** monograma **FS** (*Freestyle*) construido dentro de un escudo /
emblema. Las dos letras están integradas en una sola forma cerrada, separadas por
un corte vertical, con la punta inferior en diagonal.

**Rasgos formales:**

| Rasgo | Observación | Consecuencia en la interfaz |
|---|---|---|
| Construcción | Geométrica, a base de cortes rectos y diagonales de 45° | Radios de borde **pequeños**; nada de formas tipo píldora |
| Ángulos | Vértices agudos, sin una sola curva | Esquinas de 4–6 px como máximo |
| Contraste | Extremo: blanco puro sobre negro puro | Interfaz de alto contraste, jerarquía por peso y no por color |
| Peso | Trazos gruesos y macizos | Tipografía de títulos con peso 600–800 |
| Cromatismo | **Monocromo**, sin ningún color de acento | La paleta se apoya en neutros; el color se reserva para significar algo |
| Estilo | Streetwear / urbano / deportivo, sin adornos | Diseño sobrio, denso en información, sin decoración gratuita |

**Colores medidos sobre el archivo** (muestreo real de píxeles, no estimación):

| Color | HEX | Presencia | Papel |
|---|---|---|---|
| Negro puro | `#000000` | 91.0 % | Fondo del emblema |
| Blanco frío | `#F6F9FE` | 7.0 % | Trazo del monograma |

El blanco del logo **no es blanco puro**: es `rgb(246, 249, 254)`, un blanco con
tinte azul de matiz **217.5°**. Es el único dato cromático que da la marca, y de
ahí se deriva todo el color del sistema en lugar de inventar una paleta ajena.

---

## 2. Paleta

### Colores de marca (tomados directamente del logo)

```css
--brand-black:   #000000;  /* fondo del emblema */
--brand-white:   #F6F9FE;  /* trazo del monograma — matiz 217.5° */
```

### Acento

El logo no tiene acento. Se deriva uno **del mismo matiz 217.5°** del blanco de
marca, saturándolo: así el color de acción pertenece a la familia cromática de la
marca en vez de ser un añadido arbitrario.

```css
--brand-accent:        #1669F3;  /* hsl(217.5, 90%, 52%) */
--brand-accent-hover:  #0B57D5;
--brand-accent-soft:   #E8F0FE;  /* fondos de estado seleccionado */
```

### Neutros

Escala fría construida sobre el mismo matiz, para que grises y blancos no
"desentonen" con el logo:

```css
--neutral-0:   #FFFFFF;
--neutral-25:  #F6F9FE;  /* = blanco de marca: fondo de la aplicación */
--neutral-50:  #F1F4F9;
--neutral-100: #E5EAF2;
--neutral-200: #D3DAE6;
--neutral-300: #B4BECD;
--neutral-400: #8A94A6;
--neutral-500: #667085;
--neutral-600: #4A5265;
--neutral-700: #333A49;
--neutral-800: #1F242E;
--neutral-900: #12151B;
--neutral-950: #0A0C10;
```

### Roles semánticos

```css
--color-background:      var(--neutral-25);   /* lienzo de trabajo */
--color-surface:         var(--neutral-0);    /* tarjetas, tablas, modales */
--color-surface-sunken:  var(--neutral-50);   /* cabeceras de tabla, zonas hundidas */
--color-border:          var(--neutral-200);
--color-border-strong:   var(--neutral-300);
--color-text:            var(--neutral-900);
--color-text-secondary:  var(--neutral-600);
--color-text-muted:      var(--neutral-500);
--color-text-inverse:    var(--brand-white);

/* Navegación: usa el negro de marca, tal como el logo fue diseñado */
--color-nav-bg:          var(--brand-black);
--color-nav-surface:     #14171D;
--color-nav-text:        #A8B0BF;
--color-nav-text-active: var(--brand-white);
--color-nav-border:      #23272F;
```

### Estados

Cada estado tiene tres tonos: fondo suave, borde y texto. El texto siempre supera
4.5:1 sobre su fondo suave (WCAG AA).

```css
--color-success:      #0E9F6E;  --color-success-bg: #E3F7EF;  --color-success-text: #06674A;
--color-warning:      #D97706;  --color-warning-bg: #FDF3E4;  --color-warning-text: #92500A;
--color-danger:       #DC2626;  --color-danger-bg:  #FDECEC;  --color-danger-text:  #A31515;
--color-info:         #1669F3;  --color-info-bg:    #E8F0FE;  --color-info-text:    #0B4BB8;
```

### Estados de negocio → color

Ninguno se elige al azar; cada uno significa una acción distinta para quien atiende:

| Estado | Color | Razón |
|---|---|---|
| Activo · Completado | success | Todo correcto, sin intervención |
| Pendiente | warning | Requiere atención |
| Stock bajo | warning | Hay que reponer pronto |
| Agotado | danger | Impide vender |
| Inactivo | neutral-400 | Existe pero no opera; no debe llamar la atención |
| Cancelado · Anulado | danger | Operación revertida |
| Bloqueado | danger | Acceso impedido |
| Devuelto | info | Informativo, ni bueno ni malo |

---

## 3. Uso del logo

| Fondo | Versión | Motivo |
|---|---|---|
| Sidebar y topbar oscuros | Logo original (blanco sobre negro) | Es su uso nativo |
| Superficies claras | Monograma en `--brand-black` sobre transparente | Mantiene el contraste |
| Login | Logo original centrado sobre panel negro | Máximo impacto |
| Favicon / etiquetas | Solo el monograma, sin texto | Legible a tamaño pequeño |

**Prohibido:** deformar las proporciones, rotarlo, recolorearlo con el acento,
aplicarle sombras o degradados, y colocarlo sobre fotografías sin una capa de
contraste. Área de respeto mínima: la altura de la letra F por cada lado.

Tamaño mínimo: 24 px de alto (icono), 32 px en el sidebar contraído.

---

## 4. Tipografía

| Uso | Fuente | Por qué |
|---|---|---|
| Títulos y logotipo | **Archivo** (600 / 700) | Grotesca robusta y ligeramente condensada; recoge el peso y la angularidad del monograma sin imitarlo |
| Interfaz y cuerpo | **Inter** (400 / 500 / 600) | Diseñada para pantalla y para texto pequeño: es la que hace legible una tabla de inventario |
| Cifras y dinero | Inter con `font-variant-numeric: tabular-nums` | Los importes se alinean en columna; sin esto, los decimales bailan |
| Códigos y SKU | **JetBrains Mono** (500) | `POL-00125-M-NEG` y `7750000001255` se leen y se comparan mejor en monoespaciada |

Ambas son de código abierto y se sirven **localmente** desde `front/assets/fonts/`,
no desde un CDN externo: el POS debe funcionar aunque la conexión falle.

### Escala tipográfica

```css
--font-size-xs:   0.75rem;   /*  12px — etiquetas, badges           */
--font-size-sm:   0.8125rem; /*  13px — tablas, texto secundario    */
--font-size-base: 0.875rem;  /*  14px — base de la interfaz         */
--font-size-md:   1rem;      /*  16px — inputs (evita el zoom iOS)  */
--font-size-lg:   1.125rem;  /*  18px — títulos de sección          */
--font-size-xl:   1.375rem;  /*  22px — títulos de página           */
--font-size-2xl:  1.75rem;   /*  28px — cifras del dashboard        */
--font-size-3xl:  2.25rem;   /*  36px — total del POS               */
```

Base de 14 px, no 16: es una herramienta de trabajo con mucha información en
pantalla. Los inputs sí van a 16 px porque por debajo de ese tamaño iOS hace zoom
automático al enfocarlos, lo que rompería el POS en tablet.

---

## 5. Espaciado, bordes y elevación

```css
/* Escala de 4px */
--space-1: 0.25rem;  --space-2: 0.5rem;   --space-3: 0.75rem;
--space-4: 1rem;     --space-5: 1.25rem;  --space-6: 1.5rem;
--space-8: 2rem;     --space-10: 2.5rem;  --space-12: 3rem;

/* Radios contenidos: el logo no tiene una sola curva */
--radius-sm: 4px;    --radius-md: 6px;    --radius-lg: 8px;
--radius-full: 999px;  /* solo para avatares y puntos de estado */

/* Elevación mínima: se separa por borde, no por sombra */
--shadow-xs: 0 1px 2px rgba(10, 12, 16, .05);
--shadow-sm: 0 1px 3px rgba(10, 12, 16, .08), 0 1px 2px rgba(10, 12, 16, .04);
--shadow-md: 0 4px 12px rgba(10, 12, 16, .08);
--shadow-lg: 0 12px 32px rgba(10, 12, 16, .12);  /* solo modales y drawers */
```

Las tarjetas se delimitan con `1px solid var(--color-border)`, no con sombra. Una
pantalla con veinte elementos flotando cansa; con bordes finos se lee como un
plano ordenado.

---

## 6. Layout

```
┌────────────────────────────────────────────────────────┐
│ TOPBAR  56px   buscador · caja activa · usuario        │
├───────────────┬────────────────────────────────────────┤
│ SIDEBAR       │  CONTENT                               │
│ 248px         │  max-width 1440px · padding 24px       │
│ (72px         │                                        │
│  contraído)   │                                        │
└───────────────┴────────────────────────────────────────┘
```

- **Sidebar** sobre `--color-nav-bg` (el negro de marca) con el logo original.
  Expandido 248 px, contraído 72 px (solo iconos, con tooltip).
  El elemento activo se marca con una barra vertical de 3 px en `--brand-accent`
  más el texto en `--color-nav-text-active`.
- **Topbar** claro, con borde inferior. Muestra permanentemente el estado de la
  caja, porque de él depende poder vender.
- **Contenido** sobre `--color-background`; las tarjetas en `--color-surface`.

### Puntos de ruptura

```css
--bp-sm:  640px;   /* móvil                    */
--bp-md:  768px;   /* tablet vertical          */
--bp-lg:  1024px;  /* tablet horizontal        */
--bp-xl:  1280px;  /* escritorio               */
--bp-2xl: 1536px;  /* escritorio grande        */
```

| Ancho | Comportamiento |
|---|---|
| < 768 px | Sidebar como drawer superpuesto; las tablas se reorganizan en tarjetas apiladas, **no se ocultan columnas** |
| 768–1024 px | Sidebar contraído a iconos; POS con el carrito en pestaña |
| > 1024 px | Sidebar completo; POS a dos columnas (catálogo + carrito) |

El POS se diseña primero para **tablet horizontal**, que es la resolución real de
un mostrador, y desde ahí se adapta hacia arriba y hacia abajo.

---

## 7. Componentes

Los archivos CSS se organizan así:

```
front/css/
├── tokens.css        variables: color, tipografía, espaciado, radios, sombras
├── reset.css         normalización
├── base.css          tipografía base, foco visible, utilidades
├── layout.css        sidebar, topbar, contenido, rejillas
├── components.css    botones, inputs, tablas, cards, modales, badges, toasts
├── pages.css         ajustes específicos por pantalla (POS, dashboard)
└── responsive.css    puntos de ruptura
```

**Ningún color literal fuera de `tokens.css`.** Cambiar la paleta debe ser editar
un archivo.

### Botones

| Variante | Uso | Aspecto |
|---|---|---|
| `primary` | Acción principal (una por pantalla) | Fondo `--brand-accent`, texto blanco |
| `secondary` | Acción alternativa | Fondo `--color-surface`, borde |
| `ghost` | Acciones de tabla | Sin fondo hasta el hover |
| `danger` | Anular, eliminar | Fondo `--color-danger` |
| `dark` | Confirmar venta en POS | Fondo `--brand-black` — el gesto más contundente del sistema |

Alturas: `sm` 32 px · `md` 38 px · `lg` 44 px · `xl` 56 px (botones del POS,
pensados para pulsar con el dedo).

### Inventario de componentes

Button · Input · Select · Textarea · Checkbox · Radio · Switch · Modal · Drawer ·
Dropdown · Table · Pagination · SearchBar · FilterBar · Badge · Toast · Alert ·
Card · StatCard · Tabs · Breadcrumb · DateRangePicker · Spinner · Skeleton ·
EmptyState · ConfirmDialog · Avatar · Tooltip.

Todos consumen exclusivamente los tokens; ninguno define color propio.

---

## 8. Accesibilidad

- Contraste mínimo 4.5:1 en texto y 3:1 en elementos de interfaz (WCAG AA).
- Foco visible **siempre**: `outline: 2px solid var(--brand-accent); outline-offset: 2px`.
  Nunca `outline: none` sin sustituto.
- Todo `<input>` con su `<label>` asociado por `for`/`id`.
- El color nunca es el único portador de significado: los badges llevan texto, y
  los estados críticos, además, icono.
- Modales con foco atrapado, cierre con `Escape` y devolución del foco al abrir.
- Navegación completa por teclado, con `skip link` al contenido principal.
- Objetivos táctiles de 44 × 44 px mínimo en el POS.

---

## 9. Movimiento

Transiciones cortas y solo sobre `transform` y `opacity`, que el navegador puede
componer sin recalcular el diseño:

```css
--transition-fast: 120ms cubic-bezier(.4, 0, .2, 1);   /* hover, foco   */
--transition-base: 180ms cubic-bezier(.4, 0, .2, 1);   /* menús, tabs   */
--transition-slow: 240ms cubic-bezier(.4, 0, .2, 1);   /* modales       */
```

Nada anima por encima de 240 ms: en un POS, la animación se percibe como lentitud.
Se respeta `prefers-reduced-motion: reduce` desactivando toda transición.

---

## 10. Tema oscuro

No se implementa en la Fase 1, pero los tokens ya están preparados: todas las
decisiones de color pasan por roles semánticos (`--color-surface`, `--color-text`),
nunca por valores literales. Activarlo consistirá en redefinir ese bloque de
variables bajo `[data-theme="dark"]`, sin tocar un solo componente.
