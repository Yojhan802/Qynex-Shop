# Freestyle Perú — Sistema Integral de Gestión

Sistema interno de gestión para tiendas de ropa urbana/streetwear: catálogo de
productos con variantes (color/talla), inventario multi-almacén, punto de
venta (POS), caja, clientes, devoluciones, reportes y administración de
usuarios/roles/permisos.

- **Backend**: `aplicacion/` — Spring Boot 4.1 / Java 17 / MySQL, arquitectura
  por dominio (no por capas), JWT con permisos embebidos en el token.
- **Frontend**: `front/` — HTML/CSS/JS plano (sin framework ni build step),
  sistema de diseño propio.
- **Documentación de diseño**: `docs/01-requisitos.md` a `docs/07-seguridad.md`.

## Requisitos

- Java 17
- MySQL 8.0+ (se usó 9.3 en desarrollo; 8.4+ es totalmente compatible)
- Un servidor estático simple para el frontend en desarrollo (se usó
  `python -m http.server`, pero cualquier servidor estático sirve)
- Docker + Docker Compose (opcional, para levantar todo con un solo comando)

## Desarrollo local (sin Docker)

### Backend

```bash
cd aplicacion
cp .env.example .env   # completar con tus credenciales de MySQL local
# En bash/PowerShell, exportar las variables del .env antes de levantar:
set -a && source .env && set +a
./mvnw spring-boot:run
```

El backend queda escuchando en `http://localhost:8080`. Las migraciones de
Flyway (`src/main/resources/db/migration`) crean el esquema y siembran un
usuario administrador inicial:

```
usuario:     admin
contraseña:  FreestylePeru#2026
```

> **Cambiar esta contraseña inmediatamente en cualquier entorno real.** Es
> solo para arrancar el sistema la primera vez; el usuario queda marcado con
> `must_change_password`.

### Frontend

```bash
cd front
python -m http.server 8321
```

Abrir `http://localhost:8321`. El cliente HTTP (`js/core/api.js`) detecta este
puerto y apunta automáticamente al backend en `http://localhost:8080`; fuera
de este puerto (por ejemplo, servido por nginx en producción) usa rutas
relativas (`/api`, `/uploads`) asumiendo que hay un reverse proxy delante.

### Tests del backend

```bash
cd aplicacion
./mvnw test
```

Corren contra H2 en memoria (perfil `test`), sin necesidad de MySQL.

## Despliegue con Docker

```bash
cp .env.example .env   # completar con secretos reales, nunca los de ejemplo
docker compose up --build -d
```

Esto levanta tres servicios:

| Servicio   | Descripción                                                        |
|------------|---------------------------------------------------------------------|
| `mysql`    | Base de datos, con volumen persistente `mysql_data`                |
| `backend`  | API Spring Boot, migra el esquema automáticamente al iniciar       |
| `frontend` | nginx sirviendo el frontend estático + reverse proxy a `/api` y `/uploads` |

La aplicación queda disponible en `http://localhost:${HTTP_PORT}` (por
defecto `80`). El navegador solo habla con `frontend`; `backend` y `mysql`
quedan en la red interna de Docker, sin puertos publicados al host.

### Variables de entorno

Ver `.env.example` para la lista completa. Ninguna tiene un valor real por
defecto para secretos (`DB_PASSWORD`, `DB_ROOT_PASSWORD`, `JWT_SECRET`) — hay
que generarlos antes de desplegar, por ejemplo:

```bash
openssl rand -base64 48   # para JWT_SECRET
openssl rand -base64 24   # para contraseñas de MySQL
```

### HTTPS

`docker-compose.yml` expone el frontend en HTTP plano dentro de la red local.
Para producción real con dominio propio, poner delante un reverse proxy con
TLS (Caddy, Traefik o nginx + certbot) que termine HTTPS y reenvíe a este
`frontend` por HTTP interno — no se generan certificados falsos aquí.

### Backups

MySQL persiste en el volumen `mysql_data`. Para un backup puntual:

```bash
docker compose exec mysql sh -c 'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" '"$DB_NAME"'' > backup.sql
```

### Logs

Backend y nginx registran en stdout/stderr (estándar en contenedores):

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

## Estructura del proyecto

```
aplicacion/     Backend Spring Boot (Dockerfile incluido)
front/          Frontend estático (Dockerfile + nginx.conf incluidos)
docs/           Documentación de diseño (requisitos, arquitectura, modelo de
                datos, reglas de negocio, API, identidad visual, seguridad)
docker-compose.yml
.env.example
```
