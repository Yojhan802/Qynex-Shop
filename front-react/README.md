# Qynex frontend

Frontend único de Qynex Shop: la tienda de cara al público, el panel de
administración de cada empresa y el panel de plataforma (`/plataforma`).
React 19 + TypeScript sobre Vite.

Sustituye por completo al frontend original en HTML/CSS/JS plano, que quedó
dado de baja y fuera del repositorio.

## Desarrollo local

```text
npm ci
npm run dev
```

La aplicación queda en `http://localhost:8093`. Vite envía `/api` y `/uploads`
a `http://localhost:8080` (el backend directo); `VITE_API_PROXY_TARGET` permite
apuntar a otro destino, por ejemplo el nginx de Docker. Al ir por proxy, el
origen del navegador no cambia, así que no se desactiva CORS ni se altera la
resolución de tenant por subdominio.

## Build

```text
npm run check    # tsc --noEmit
npm run build    # typecheck + vite build
```

## Organización

```text
index.html          punto de entrada
public/assets/      imágenes servidas tal cual desde la raíz
src/
  base/             CSS del design system (ver docs/06-identidad-visual.md)
  assets/fonts/     Archivo, Inter y JetBrains Mono; bajo src/ para que Vite
                    las empaquete con hash y se puedan cachear de por vida
  components/       piezas reutilizables entre pantallas
  pages/            una por pantalla
  templates/        las 10 plantillas de tienda (CLASSIC, MINIMAL,
                    FASHION, BOUTIQUE, CATALOG, EDITORIAL, LUXURY,
                    MARKET, SPORT, URBAN) + superficies compartidas
  services/         api, auth, carrito, SSE, ubigeo, validación, legal
  data/             datos estáticos (ubigeo del Perú)
```

## Decisiones de seguridad y UX

- Se mantienen separadas las sesiones `fsp.session` y `fsp.customer.session`.
- El precio real y el stock siguen siendo responsabilidad del backend.
- El carrito usa la clave `fsp.customer.cart`.
- El checkout mantiene métodos manuales y las tres pasarelas existentes.
- La plantilla se resuelve por configuración del tenant, con fallback seguro a
  `CLASSIC` y sin flash de contenido antes de marcarla como lista.
- Los controles respetan teclado, foco visible, 44 px mínimos,
  `prefers-reduced-motion` y recomposición responsive.

## Docker

La imagen se construye con esta carpeta como contexto, sin depender del resto
del repositorio:

```text
docker compose up -d --build frontend-react
```

Queda en `http://localhost:8093` (`REACT_HTTP_PORT` cambia el puerto). El
`nginx.conf` sirve el estático y hace de reverse proxy a `/api` y `/uploads`,
así que el navegador habla con un solo origen.
