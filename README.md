# Freestyle Perú — Sistema Integral de Gestión

Sistema interno de gestión para tiendas de ropa urbana/streetwear: catálogo de
productos con variantes (color/talla), inventario multi-almacén, punto de
venta (POS) con pago mixto y devoluciones, caja, clientes, promotores de piso
con reporte de comisión, reportes con exportación CSV, y administración de
usuarios/roles/permisos.

- **Backend**: `aplicacion/` — Spring Boot 4.1 / Java 17 / MySQL, arquitectura
  por dominio (no por capas), JWT con permisos embebidos en el token.
- **Frontend**: `front/` — HTML/CSS/JS plano (sin framework ni build step),
  sistema de diseño propio.
- **Documentación de diseño**: `docs/01-requisitos.md` a `docs/09-desarrollo.md`.

## Empezar rápido

```bash
# Backend
cd aplicacion && cp .env.example .env   # completar con tu MySQL local
set -a && source .env && set +a && ./mvnw spring-boot:run

# Frontend (en otra terminal)
cd front && python -m http.server 8321
```

Abrir `http://localhost:8321`. Usuario inicial: `admin` / `FreestylePeru#2026`
(pide cambio de contraseña al primer ingreso). Guía completa, tests y
convenciones del proyecto en **[docs/09-desarrollo.md](docs/09-desarrollo.md)**.

## Desplegar con Docker

```bash
cp .env.example .env   # completar con secretos reales, nunca los de ejemplo
docker compose up --build -d
```

Levanta MySQL + backend + nginx (reverse proxy y único puerto publicado).
Detalle de arquitectura, HTTPS, backups y logs en
**[docs/08-despliegue.md](docs/08-despliegue.md)**.

## Estructura del proyecto

```
aplicacion/     Backend Spring Boot (Dockerfile incluido)
front/          Frontend estático (Dockerfile + nginx.conf incluidos)
docs/           Documentación de diseño (requisitos, arquitectura, modelo de
                datos, reglas de negocio, API, identidad visual, seguridad,
                despliegue, desarrollo)
docker-compose.yml
.env.example
```
