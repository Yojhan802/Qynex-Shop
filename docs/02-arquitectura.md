# 02 — Arquitectura

## 1. Visión general

```
┌──────────────────────────────────────────────────────────┐
│  FRONTEND  ·  React 19 + TypeScript (Vite)               │
│  front-react/                                            │
│  SPA · Design System propio · tienda + panel + plataforma│
└───────────────────────────┬──────────────────────────────┘
                            │  HTTPS · JSON · JWT Bearer
┌───────────────────────────▼──────────────────────────────┐
│  BACKEND  ·  Spring Boot 4.1 (Java 17)                   │
│  aplicacion/                                             │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │ web        controllers REST + validación entrada   │  │
│  ├────────────────────────────────────────────────────┤  │
│  │ service    reglas de negocio + @Transactional      │  │
│  ├────────────────────────────────────────────────────┤  │
│  │ repository Spring Data JPA                         │  │
│  ├────────────────────────────────────────────────────┤  │
│  │ domain     entidades JPA + enums                   │  │
│  └────────────────────────────────────────────────────┘  │
│                                                          │
│  Transversal: security · exception · audit · config      │
└───────────────────────────┬──────────────────────────────┘
                            │  JDBC
┌───────────────────────────▼──────────────────────────────┐
│  MySQL 8  ·  esquema versionado con Flyway               │
└──────────────────────────────────────────────────────────┘
```

## 2. Principio organizativo: paquete por dominio

El código se organiza **por dominio de negocio**, no por tipo técnico. Cada
módulo es autónomo y contiene sus propias capas.

**Por qué:** un CRUD organizado por capas (`controllers/`, `services/`,
`repositories/`…) obliga a saltar entre carpetas lejanas para tocar una sola
funcionalidad, y a medida que crece se vuelve inmanejable. Agrupando por dominio,
todo lo de "ventas" vive junto, las fronteras entre módulos se ven a simple
vista, y extraer un módulo a un servicio independiente en el futuro es viable.

```
com.freestyleperu.aplicacion
│
├── AplicacionApplication.java
│
├── shared/                        ← transversal, sin lógica de negocio
│   ├── config/                    JPA auditing, CORS, OpenAPI, Jackson, zona horaria
│   ├── exception/                 excepciones de negocio + handler global
│   ├── security/                  JWT, filtros, UserDetails, SecurityConfig
│   ├── audit/                     servicio y aspecto de auditoría
│   ├── domain/                    BaseEntity, EstadoGeneral, tipos comunes
│   ├── dto/                       ApiError, PageResponse, respuestas comunes
│   └── util/                      generadores de SKU, EAN-13, correlativos
│
├── auth/                          login, refresh, cambio de contraseña
│   ├── web/  service/  dto/
│
├── usuario/                       usuarios, roles, permisos
│   ├── domain/    Usuario, Rol, Permiso, UsuarioEstado
│   ├── repository/
│   ├── service/
│   ├── web/
│   ├── dto/
│   └── mapper/
│
├── catalogo/                      categorías, subcategorías, marcas, colores, tallas
├── producto/                      productos y variantes
├── inventario/                    movimientos, ajustes, almacenes
├── cliente/                       clientes e historial
├── venta/                         ventas, detalle, anulaciones
├── pago/                          métodos de pago y pagos
├── caja/                          cajas, sesiones, movimientos
├── devolucion/                    devoluciones y su detalle
├── reporte/                       consultas agregadas de solo lectura
└── configuracion/                 empresa, parámetros del sistema
```

Cada módulo de dominio sigue la misma forma interna:

```
<modulo>/
├── domain/       entidades JPA y enums del dominio
├── repository/   interfaces Spring Data
├── service/      reglas de negocio, transacciones
├── web/          @RestController
├── dto/          request/ y response/
└── mapper/       conversión entidad ↔ DTO
```

## 3. Reglas de dependencia entre capas

```
web  →  service  →  repository  →  domain
                 ↘  mapper  ↗
```

Invariantes que el código debe respetar siempre:

1. **Un controller nunca toca un repository.** Siempre pasa por un service.
2. **Un controller nunca devuelve una entidad JPA.** Siempre un DTO de respuesta.
3. **La lógica de negocio vive en el service**, no en el controller ni en la entidad.
4. **Las transacciones se abren en el service**, nunca en el controller.
5. **Los módulos se comunican vía service**, no accediendo al repository ajeno.
6. **`shared` no depende de ningún módulo de dominio.** La flecha va solo hacia dentro.

## 4. Comunicación entre módulos

Dependencias reales entre dominios (todas hacia el service público del otro módulo):

```
venta ──→ producto     (validar variante y precio)
venta ──→ inventario   (descontar stock generando movimiento)
venta ──→ caja         (registrar movimiento de efectivo)
venta ──→ pago         (registrar pagos y validar la suma)
venta ──→ cliente      (asociar cliente opcional)

devolucion ──→ venta       (venta original)
devolucion ──→ inventario  (reingreso de stock si corresponde)
devolucion ──→ caja        (salida de efectivo si corresponde)

todos ──→ audit  (registro de operaciones sensibles)
```

`VentaService` es el orquestador de la operación más crítica. Coordina a los
demás dentro de **una única transacción**.

## 5. Transaccionalidad

La venta es la operación con más efectos colaterales. Todo ocurre dentro de un
mismo `@Transactional`; si algo falla, no queda nada a medias:

```
@Transactional
registrarVenta()
  1. validar sesión de caja abierta
  2. validar variantes, precios y stock disponible
  3. bloquear variantes con PESSIMISTIC_WRITE   ← evita sobreventa concurrente
  4. crear venta + detalle (con snapshot de nombres y precios)
  5. validar que suma(pagos) == total
  6. registrar pagos
  7. descontar stock generando un movimiento por línea
  8. registrar movimiento de caja si hubo efectivo
  9. registrar auditoría
```

**Concurrencia:** dos cajeros vendiendo la última unidad simultáneamente es un
caso real. Se resuelve con bloqueo pesimista sobre la variante
(`SELECT ... FOR UPDATE`) ordenando siempre los IDs de forma ascendente para
evitar interbloqueos.

## 6. Seguridad

- **Autenticación:** JWT firmado con HMAC-SHA256. Access token de vida corta
  (30 min) + refresh token persistido en base de datos y revocable.
- **Autorización:** basada en **permisos**, no en el nombre del rol. Cada permiso
  (`VENTAS_ANULAR`) se convierte en un `GrantedAuthority`, y los endpoints usan
  `@PreAuthorize("hasAuthority('VENTAS_ANULAR')")`. Añadir un rol nuevo no obliga
  a tocar código.
- **Contraseñas:** BCrypt con factor 12.
- **Sin estado:** `SessionCreationPolicy.STATELESS`, sin sesión HTTP.
- **CORS:** orígenes permitidos por configuración, nunca `*` con credenciales.
- **Secretos:** exclusivamente por variables de entorno. El repositorio incluye
  `.env.example` sin valores reales.

Detalle completo en [07-seguridad.md](07-seguridad.md).

## 7. Manejo de errores

Una única jerarquía de excepciones de negocio y un `@RestControllerAdvice` que
las traduce a respuestas HTTP uniformes:

| Excepción | HTTP | Código |
|---|---|---|
| `RecursoNoEncontradoException` | 404 | `RESOURCE_NOT_FOUND` |
| `ReglaDeNegocioException` | 409 | `BUSINESS_RULE_VIOLATION` |
| `RecursoDuplicadoException` | 409 | `DUPLICATE_RESOURCE` |
| `StockInsuficienteException` | 409 | `INSUFFICIENT_STOCK` |
| `OperacionNoPermitidaException` | 403 | `OPERATION_NOT_ALLOWED` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `AccessDeniedException` | 403 | `ACCESS_DENIED` |
| Cualquier otra | 500 | `INTERNAL_ERROR` |

Respuesta uniforme, sin stack traces:

```json
{
  "timestamp": "2026-08-19T18:32:11-05:00",
  "status": 409,
  "error": "DUPLICATE_RESOURCE",
  "message": "El código de barras 7750000000012 ya está registrado",
  "path": "/api/variants",
  "fieldErrors": []
}
```

## 8. Auditoría

Servicio transversal (`AuditoriaService`) invocado explícitamente desde los
services de negocio en las operaciones sensibles. Se registra en `audit_logs`
con `REQUIRES_NEW`, de modo que **la auditoría persiste aunque la transacción
principal se revierta** — así queda constancia de los intentos fallidos y
denegados, que es justo lo que interesa auditar.

## 9. Base de datos y migraciones

- **Flyway** con scripts SQL versionados en `src/main/resources/db/migration`.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **nunca** modifica el
  esquema; solo verifica que las entidades coincidan con las tablas reales.
- Índices, claves únicas y foráneas se declaran explícitamente en el SQL.
- Los datos semilla (permisos, roles, catálogos) van en migraciones aparte.

Detalle en [03-modelo-datos.md](03-modelo-datos.md).

## 10. Preparación para fases futuras

| Fase futura | Qué la habilita ya en el diseño actual |
|---|---|
| Multisucursal | Tablas `branches` y `warehouses` existentes; movimientos e inventario ya referencian almacén; cajas referencian sucursal |
| Ecommerce | El producto ya tiene descripción, imagen, precio promocional y estado; el stock se consulta por API |
| API pública | Los controllers ya devuelven DTOs estables, desacoplados de las entidades |
| IA / WhatsApp | Endpoints de consulta de catálogo y stock que devuelven datos reales; la IA nunca inventa porque consulta la misma API |
| Aplicación móvil | Backend sin estado con JWT: cualquier cliente puede consumirlo |

**Ninguna de estas fases se implementa ahora.** El diseño simplemente no las
bloquea.

## 11. Decisiones arquitectónicas y sus alternativas

### D-01 · Paquete por dominio en vez de por capa
Alternativa descartada: paquete por capa técnica. Escala mal y dispersa cada
funcionalidad. **Elegido:** por dominio, con capas dentro de cada módulo.

### D-02 · Flyway en vez de `ddl-auto=update`
`ddl-auto=update` no controla índices ni restricciones, nunca elimina columnas y
es impredecible entre entornos. **Elegido:** Flyway con `validate`.

### D-03 · Autorización por permiso, no por rol
`hasRole('ADMIN')` obliga a recompilar cada vez que cambia la política.
**Elegido:** permisos como authorities; los roles son solo agrupaciones de permisos.

### D-04 · Stock materializado en la variante
El documento lo pide explícitamente (§11). La alternativa pura sería derivar el
stock sumando movimientos, que es más lento en el POS.
**Elegido:** columna `stock` en `product_variants` como saldo, **siempre**
actualizada dentro de la misma transacción que crea el movimiento. Los
movimientos siguen siendo la fuente de verdad auditable, y su suma debe cuadrar
con la columna. Cuando llegue el multi-almacén, el saldo se traslada a
`inventory_stock(variant_id, warehouse_id)` sin tocar los movimientos, que ya
llevan `warehouse_id`.

### D-05 · Snapshot de datos en el detalle de venta
El detalle guarda copia del nombre del producto, SKU, color, talla y precio del
momento de la venta. Si mañana renombran o cambian el precio de un producto, las
ventas históricas y los reportes siguen mostrando lo que realmente se vendió.

### D-06 · Mappers manuales en vez de MapStruct
MapStruct genera código en tiempo de compilación y hay que coordinarlo con el
procesador de anotaciones de Lombok, lo que añade un punto de fallo en el build.
Con el volumen de DTOs de este proyecto, mappers como `@Component` de Spring son
explícitos, depurables y no aportan fricción. **Reversible** si el proyecto crece.

### D-07 · Correlativos en tabla `sequences`
`AUTO_INCREMENT` no sirve para números de venta con formato `V001-00000123` ni
para SKU legibles. Una tabla de secuencias con bloqueo pesimista da control del
formato y evita huecos por transacciones revertidas.

## 12. Estructura de carpetas del repositorio

```
FreestylePeru/aplicacion/
├── documento.txt              especificación original del cliente
├── recursos/                  material de marca (logo)
├── docs/                      documentación técnica (Fase 0)
│   ├── 01-requisitos.md
│   ├── 02-arquitectura.md
│   ├── 03-modelo-datos.md
│   ├── 04-reglas-negocio.md
│   ├── 05-api.md
│   ├── 06-identidad-visual.md
│   └── 06-seguridad.md
├── aplicacion/                backend Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/freestyleperu/aplicacion/
│       └── resources/
│           ├── application.yml
│           └── db/migration/
└── front-react/               frontend React + Vite
    ├── index.html
    ├── public/assets/          servidos tal cual desde la raíz
    └── src/
        ├── base/               CSS del design system
        ├── components/
        ├── pages/
        ├── templates/          plantillas de tienda
        └── services/           cliente HTTP y dominios
```
