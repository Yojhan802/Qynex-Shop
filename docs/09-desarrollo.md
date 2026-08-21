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
cd front
python -m http.server 8321
```

`front/js/core/api.js` detecta el puerto `8321` específicamente y por eso
apunta al backend en `http://localhost:8080`; en cualquier otro puerto (por
ejemplo, servido por nginx en Docker) usa rutas relativas, asumiendo que hay
un reverse proxy delante. Si cambias el puerto del servidor estático local,
hay que actualizar esa constante.

### Caché del navegador tras cada cambio

El servidor estático no envía cabeceras que invaliden la caché del navegador,
así que después de editar un archivo `.js` puede hacer falta un refresco
forzado (Ctrl+Shift+R) para ver el cambio — el navegador puede seguir
sirviendo la versión anterior desde caché.

### Verificar cambios visuales

No hay suite de tests de frontend. Los cambios de UI se verifican
manualmente con Playwright contra el backend y la base de datos reales
(nunca mocks) — ver el patrón usado en las últimas sesiones de desarrollo:
un script descartable que hace login, navega, interactúa, y toma capturas
para revisar antes de darlo por bueno.

## 5. Estructura del proyecto

```
aplicacion/     Backend Spring Boot — paquetes por dominio (no por capa):
                cada módulo (venta, inventario, caja, …) trae su propio
                domain/repository/service/dto/web
front/          Frontend estático — sin build step, sin framework.
                js/core/    utilidades compartidas (api, auth, format…)
                js/components/  piezas de UI reutilizables entre páginas
                js/pages/   un archivo por página, bootstrap al final del
                            archivo (evita un bug de TDZ ya encontrado una vez)
docs/           Esta documentación de diseño
docker-compose.yml, aplicacion/Dockerfile, front/Dockerfile, front/nginx.conf
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
