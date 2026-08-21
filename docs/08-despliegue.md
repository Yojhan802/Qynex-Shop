# 08 — Despliegue

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
