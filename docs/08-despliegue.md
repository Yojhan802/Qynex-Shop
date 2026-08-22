# 08 — Despliegue

**Dos caminos documentados:** este archivo describe primero el despliegue
con `docker-compose.yml` (útil para desarrollo o un VPS con más recursos).
La instalación real recomendada para un VPS chico compartido (ej. OVZ VPS
2000: 2 vCPU / 2 GB RAM) es la **§8 "Despliegue nativo"** más abajo — JDK +
MySQL + nginx instalados directo, sin Docker, porque en un VPS con poca RAM
la capa de contenedores compite por memoria que le hace falta a MySQL y a
la JVM.

## 1. Arquitectura de despliegue

```
Internet
   │
   ▼
frontend (nginx, puerto público)
   ├─ sirve el HTML/CSS/JS estático
   └─ /api/**, /uploads/**  ──proxy──▶  backend (Spring Boot, red interna)
                                            │
                                            ▼
                                        mysql (red interna)
```

Solo `frontend` publica un puerto al host. `backend` y `mysql` quedan en la
red interna de Docker, sin exponerse — el navegador nunca les habla
directamente. Esto es deliberado: el frontend usa rutas relativas (`/api`,
`/uploads`, ver `front/js/core/api.js`) precisamente para poder vivir detrás
de este proxy sin configuración adicional.

## 2. Servicios (`docker-compose.yml`)

| Servicio | Imagen | Rol |
|---|---|---|
| `mysql` | `mysql:8.4` | Base de datos, volumen persistente `mysql_data` |
| `backend` | build de `aplicacion/Dockerfile` | API Spring Boot; migra el esquema (Flyway) al arrancar |
| `frontend` | build de `front/Dockerfile` | nginx: estático + reverse proxy |

`backend` espera a que `mysql` pase su healthcheck (`mysqladmin ping`) antes
de arrancar — sin esto, Spring Boot podría intentar conectarse antes de que
MySQL esté listo para aceptar conexiones. `backend` a su vez expone su propio
healthcheck contra `GET /actuator/health` (Spring Boot Actuator, solo el
endpoint `health` expuesto, sin detalle — `management.endpoint.health.show-details: never`
en `application.yml` — para no filtrar datos de conexión a un llamador
anónimo), y `frontend` espera a que `backend` esté saludable antes de
arrancar. Este mismo endpoint es el punto de apoyo para un futuro monitoreo
centralizado de todas las instalaciones (una por cliente, ver
docs/03-modelo-datos.md §15) — cada una puede reportar su estado con la misma
llamada.

## 3. Primer despliegue

```bash
cp .env.example .env   # completar con secretos reales, nunca los de ejemplo
docker compose up --build -d
```

Variables obligatorias en `.env` (ver `.env.example` para la lista completa):

| Variable | Para qué |
|---|---|
| `DB_PASSWORD`, `DB_ROOT_PASSWORD` | Credenciales de MySQL |
| `JWT_SECRET` | Firma de los access tokens — generar con `openssl rand -base64 48` |
| `CORS_ALLOWED_ORIGINS` | Solo relevante si el frontend se sirve desde otro origen que el proxy |
| `HTTP_PORT` | Puerto público del `frontend` (por defecto 80) |

Ninguna de estas tiene un valor real por defecto: si falta alguna, el
contenedor correspondiente falla al arrancar en vez de arrancar con un
secreto débil o vacío.

`OPS_API_KEY` es distinta: **opcional**, vacía por defecto. Solo hace falta
si vas a controlar el estado de pago de este cliente desde `panel-monitoreo`
(`PUT /api/system/subscription`, docs/05-api.md). Sin configurarla, ese
endpoint sigue existiendo pero queda inalcanzable para cualquiera — no rompe
el arranque, simplemente esa función queda apagada.

Al primer arranque, Flyway crea el esquema completo y siembra:

```
usuario:     admin
contraseña:  FreestylePeru#2026
```

**Cambiar esta contraseña de inmediato** en cualquier entorno real — el
usuario queda marcado con `must_change_password`, así que el sistema lo
exige en el primer login.

## 4. HTTPS

`docker-compose.yml` expone `frontend` en HTTP plano dentro de la red local.
Para un dominio real, poner delante un reverse proxy con TLS (Caddy, Traefik,
o nginx + certbot) que termine HTTPS y reenvíe a `frontend` por HTTP interno.
No se generan certificados autofirmados en este repositorio: un certificado
falso da una falsa sensación de seguridad y complica el despliegue real más
de lo que ayuda.

## 5. Backups

MySQL persiste en el volumen `mysql_data`. Backup puntual:

```bash
docker compose exec mysql sh -c 'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" '"$DB_NAME"'' > backup.sql
```

Restaurar:

```bash
docker compose exec -T mysql sh -c 'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" '"$DB_NAME"'' < backup.sql
```

No hay backup automático programado en este repositorio — en producción real,
esto debería correr como un cron fuera del contenedor (o un job del
orquestador) que además copie el `.sql` resultante a almacenamiento externo,
no solo al disco local del mismo host.

## 6. Logs

Backend y nginx registran en stdout/stderr, el estándar en contenedores — no
escriben a archivo dentro de la imagen:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

## 7. Actualizar una versión ya desplegada

```bash
git pull
docker compose up --build -d
```

Flyway solo aplica las migraciones nuevas (compara contra
`flyway_schema_history`), así que un `up --build` normal alcanza — no hace
falta bajar los contenedores ni tocar el volumen de datos.

## 8. Despliegue nativo (VPS sin Docker)

Camino recomendado para un VPS chico (ej. OVZ VPS 2000: 2 vCPU / 2 GB RAM,
~$10.88/mes) donde JDK, MySQL y nginx corren directo en el sistema
operativo, sin la capa extra de contenedores. Los archivos de referencia
viven en `deploy/`:

| Archivo | Para qué |
|---|---|
| `deploy/freestyleperu-backend.service` | Unidad systemd — arranca el `.jar`, lo reinicia solo si se cae (`Restart=on-failure`), y trae la JVM ya dimensionada para 2 GB de RAM compartidos con MySQL |
| `deploy/nginx-freestyleperu.conf` | Config de sitio nginx — estático + proxy a `127.0.0.1:8080`, con gzip y límite de tasa sobre `/api/`; incluye `location`s aparte para los streams SSE de notificaciones (`/api/notifications/`, `/api/store/notifications/`) con `proxy_buffering off` — sin eso las notificaciones en tiempo real (docs/05-api.md §22) funcionan en local y nunca llegan "en vivo" en producción |

**Pasos, en orden:**

1. Instalar JDK 17, MySQL 8 y nginx directo con el gestor de paquetes de la
   distro (ej. `apt install openjdk-17-jre-headless mysql-server nginx`).
2. Crear la base de datos y el usuario de la app en MySQL (mismas
   credenciales que `.env`).
3. `./mvnw package -DskipTests` en `aplicacion/` y copiar
   `target/*.jar` a `/opt/freestyleperu/app.jar` en el servidor (o hacer el
   build directo ahí). Copiar también `front/` a `/opt/freestyleperu/front`.
4. Crear `/opt/freestyleperu/.env` con las mismas variables de
   `aplicacion/.env.example`, agregando `SPRING_PROFILES_ACTIVE=prod` (activa
   el perfil con el HikariCP/Tomcat ya dimensionados para este VPS — ver
   `application.yml`).
5. Instalar y arrancar `deploy/freestyleperu-backend.service` (ver los
   comentarios del archivo para los comandos exactos).
6. Instalar `deploy/nginx-freestyleperu.conf` como sitio de nginx (ver sus
   propios comentarios — incluye una línea que va aparte en el
   `nginx.conf` global, no en el archivo de sitio).
7. HTTPS: igual que en el camino Docker (§4) — Caddy, Traefik, o certbot
   delante de nginx.

**Antes de un pico de tráfico grande (Black Friday):** lo de mayor impacto
y costo cero es poner **Cloudflare (plan gratuito)** delante del dominio.
Cachea los archivos estáticos y hasta respuestas de API cacheables, absorbe
ráfagas de tráfico antes de que lleguen al VPS, y da una capa básica de
protección contra abuso — nada de esto cuesta y no depende de cuánta
RAM/CPU tenga el VPS. Ninguna configuración de software reemplaza más
CPU/RAM si el tráfico real termina siendo grande — conviene además hacer
una prueba de carga externa (`k6` o `ab`) contra el VPS ya configurado, para
saber de antemano cuántos usuarios concurrentes aguanta antes de necesitar
subir de plan.
