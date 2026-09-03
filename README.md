# Qynex Shop

Plataforma **SaaS multi-empresa** de gestión y venta para tiendas de retail.
Cada empresa cliente vive en su propio subdominio, con su catálogo, su marca y
sus datos aislados, y contrata solo los módulos que necesita.

Incluye gestión interna (catálogo con atributos configurables, inventario
multi-almacén, POS con pago mixto y devoluciones, caja, clientes, promotores,
reportes) y tienda virtual de cara al público (carrito, pedidos, pasarelas de
pago, facturación electrónica SUNAT y Libro de Reclamaciones).

- **Backend**: `aplicacion/` — Spring Boot 4.1 / Java 17 / MySQL, arquitectura
  por dominio, JWT con permisos embebidos y multi-tenancy por `@TenantId`.
- **Frontend**: `front-react/` — React 19 + TypeScript + Vite. Es el único
  frontend: sirve la tienda, el panel de administración y el panel de
  plataforma.
- **Documentación de diseño**: `docs/01-requisitos.md` a `docs/10-…`.

## Empezar rápido

```bash
# Backend
cd aplicacion && cp .env.example .env   # completar con tu MySQL local
set -a && source .env && set +a && ./mvnw spring-boot:run

# Frontend (en otra terminal)
cd front-react && npm install && npm run dev
```

Abrir `http://localhost:8093`. Usuario inicial: `admin` / `FreestylePeru#2026`
(pide cambio de contraseña al primer ingreso). Guía completa, tests y
convenciones del proyecto en **[docs/09-desarrollo.md](docs/09-desarrollo.md)**.

## Desplegar con Docker

```bash
cp .env.example .env   # completar con secretos reales, nunca los de ejemplo
docker compose up --build -d
```

Levanta MySQL + backend + nginx. Ese nginx es el único puerto publicado
(`http://localhost:8093`, configurable con `REACT_HTTP_PORT`): sirve el
frontend y hace de reverse proxy a `/api`, así que el navegador habla con un
solo origen. Detalle de arquitectura, HTTPS, backups y logs en
**[docs/08-despliegue.md](docs/08-despliegue.md)**.

## Multi-empresa y módulos

Cada empresa se resuelve por el subdominio de la petición
(`mitienda.qynex.pe`). El dominio raíz no es una empresa: aloja la tienda de
demostración y el panel de plataforma en `/plataforma`, desde donde se
administran empresas, módulos contratados, renovaciones y suscripciones.

Los módulos (`ModuloSistema`) se contratan por separado y se cobran por
precio individual, así que el paquete se arma a la medida del presupuesto de
cada cliente. Los planes existen solo como preselecciones. Las dependencias
entre módulos se resuelven solas: activar POS arrastra Productos, Inventario y
Caja; activar la tienda virtual obliga el Libro de Reclamaciones, que es una
exigencia legal.

## Estructura del proyecto

```
aplicacion/     Backend Spring Boot (Dockerfile incluido)
front-react/    Frontend React + Vite (Dockerfile + nginx.conf incluidos)
docs/           Documentación de diseño (requisitos, arquitectura, modelo de
                datos, reglas de negocio, API, identidad visual, seguridad,
                despliegue, desarrollo, integraciones)
deploy/         Plantillas de nginx y systemd para el servidor
docker-compose.yml
.env.example
```

> `front/` era el frontend original en HTML/CSS/JS plano. Está dado de baja y
> fuera del repositorio desde que `front-react` lo reemplazó por completo.
