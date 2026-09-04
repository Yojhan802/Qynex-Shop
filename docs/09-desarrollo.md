# 09 — Desarrollo local

## 1. Por qué sin Docker en desarrollo

Docker es la forma de *desplegar*; para iterar día a día es más rápido correr
el backend y el frontend directo en la máquina, contra un MySQL local — sin
reconstruir una imagen por cada cambio. `docs/08-despliegue.md` cubre el
camino de producción; esta página cubre el de desarrollo.

## 2. Requisitos

- Java 17
- MySQL 8.0+ corriendo localmente (se usó 9.3 en desarrollo)
- Un servidor estático para el frontend — cualquiera sirve, se usó
  `python -m http.server`
- Node + `playwright-core` si se va a verificar el frontend contra un
  navegador real (no es una dependencia del proyecto, es una herramienta de
  verificación)

## 3. Backend

```bash
cd aplicacion
cp .env.example .env   # completar con las credenciales de tu MySQL local
set -a && source .env && set +a
./mvnw spring-boot:run
```

Queda escuchando en `http://localhost:8080`. Las migraciones de Flyway
(`src/main/resources/db/migration`) crean el esquema y siembran el usuario
`admin` / `FreestylePeru#2026` (ver `docs/08-despliegue.md` §3 sobre por qué
hay que cambiarla).

### Tests

```bash
./mvnw test
```

Corren contra H2 en memoria (perfil `test`, ver
`src/test/resources/application-test.yml`), no contra el MySQL local — así
que no hace falta tener el backend levantado ni la base de datos poblada
para correrlos. `app.uploads.dir` también apunta a un directorio temporal en
ese perfil, para que subir un archivo en un test no deje basura en el repo.

## 4. Frontend

```bash
cd front-react
npm ci        # la primera vez
npm run dev
```

Queda en `http://localhost:8093`. El cliente HTTP usa siempre rutas relativas
(`API_BASE = '/api'` en `src/services/api.ts`), así que no hay ningún puerto
codificado: en desarrollo el proxy de Vite manda `/api` y `/uploads` a
`http://localhost:8080`, y en Docker lo hace el nginx del contenedor. Para
apuntar a otro backend, `VITE_API_PROXY_TARGET`.

Antes de dar por bueno un cambio, `npm run build` — que corre `tsc --noEmit`
primero, así que un error de tipos falla el build en vez de llegar al bundle.

### Verificar cambios visuales

No hay suite de tests de frontend. Los cambios de UI se verifican
manualmente con Playwright contra el backend y la base de datos reales
(nunca mocks) — ver el patrón usado en las últimas sesiones de desarrollo:
un script descartable que hace login, navega, interactúa, y toma capturas
para revisar antes de darlo por bueno.

**Navegar no es leer.** Esa base de datos real tiene empresas reales, y en el
panel de plataforma un clic en "guardar" de un formulario ya cargado
sobrescribe el paquete, el nombre o el estado de suscripción de esa empresa.
Ya pasó: tres empresas quedaron con precios y razón social alterados por un
script de verificación. Para revisar pantallas que escriben, usar una empresa
de prueba creada para eso, o quedarse en las de solo lectura. Si aun así
tocaste una, `tenant_module_changes` guarda el paquete anterior y permite
reconstruir lo que había.

## 5. Estructura del proyecto

```
aplicacion/     Backend Spring Boot — paquetes por dominio (no por capa):
                cada módulo (venta, inventario, caja, …) trae su propio
                domain/repository/service/dto/web
front-react/    Frontend React + TypeScript sobre Vite. Es el único: sirve la
                tienda, el panel de cada empresa y el de plataforma.
                src/base/       CSS del design system
                src/services/   api, auth, carrito, SSE, ubigeo, validación
                src/components/ piezas de UI reutilizables entre pantallas
                src/pages/      una por pantalla
                src/templates/  las 10 plantillas de tienda
docs/           Esta documentación de diseño
docker-compose.yml, aplicacion/Dockerfile, front-react/Dockerfile,
front-react/nginx.conf
                Despliegue — ver docs/08-despliegue.md
```

## 6. Convenciones que no son obvias leyendo el código

- **Nunca generar el esquema con Hibernate** (`ddl-auto: validate`): todo
  cambio de esquema es una migración Flyway nueva, nunca una edición a una ya
  aplicada.
- **El stock solo se toca a través de `InventarioService.ajustarStock`**, con
  lock pesimista — ni venta ni devolución ni ajuste manual tocan la columna
  directo.
- **El precio nunca viaja desde el cliente** en una venta: `CrearVentaRequest`
  no tiene campo de precio unitario a propósito, para que no se pueda vender
  algo a un precio manipulado desde el navegador. El precio se resuelve
  siempre server-side contra el producto.
