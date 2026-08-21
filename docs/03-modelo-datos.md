# 03 — Modelo de datos

MySQL 8 · InnoDB · `utf8mb4_0900_ai_ci` · zona horaria `America/Lima`

## 1. Mapa de relaciones

```
                          ┌──────────┐
                          │ branches │  (sucursal — 1 registro en Fase 1)
                          └────┬─────┘
                 ┌─────────────┼──────────────┐
                 │                            │
          ┌──────▼──────┐            ┌────────▼────────┐
          │ warehouses  │            │ cash_registers  │
          └──────┬──────┘            └────────┬────────┘
                 │                            │
                 │                   ┌────────▼────────┐
                 │                   │  cash_sessions  │
                 │                   └────────┬────────┘
                 │                            │
                 │                   ┌────────▼────────┐
                 │                   │ cash_movements  │
                 │                   └─────────────────┘
                 │
   ┌─────────────▼─────────────┐
   │   inventory_movements     │◄──────┐
   └─────────────┬─────────────┘       │
                 │                     │ (referencia polimórfica
   ┌─────────────▼─────────────┐       │  reference_type/reference_id)
   │     product_variants      │       │
   └──┬────────┬────────┬──────┘       │
      │        │        │              │
 ┌────▼───┐ ┌──▼───┐ ┌──▼────┐         │
 │products│ │colors│ │ sizes │         │
 └───┬────┘ └──────┘ └───────┘         │
     │                                 │
 ┌───▼──────────┬──────────┐           │
 │ categories   │  brands  │           │
 └───┬──────────┴──────────┘           │
     │                                 │
 ┌───▼───────────┐                     │
 │ subcategories │                     │
 └───────────────┘                     │
                                       │
   ┌───────────┐    ┌──────────────────┴──┐    ┌──────────┐
   │ customers │◄───┤       sales         ├───►│  users   │
   └───────────┘    └───┬────────────┬────┘    └────┬─────┘
                        │            │              │
              ┌─────────▼───┐   ┌────▼─────┐   ┌────▼──────┐
              │sale_details │   │ payments │   │user_roles │
              └─────┬───────┘   └────┬─────┘   └────┬──────┘
                    │                │              │
                    │       ┌────────▼────────┐  ┌──▼────┐
                    │       │ payment_methods │  │ roles │
                    │       └─────────────────┘  └──┬────┘
                    │                               │
         ┌──────────▼──────────┐         ┌──────────▼─────────┐
         │  return_details     │         │ role_permissions   │
         └──────────┬──────────┘         └──────────┬─────────┘
                    │                               │
              ┌─────▼─────┐                   ┌─────▼───────┐
              │  returns  │                   │ permissions │
              └───────────┘                   └─────────────┘

  Independientes:  audit_logs · company_settings · sequences · refresh_tokens
```

## 2. Convenciones

- Nombres de tabla en **inglés, plural, snake_case** (coinciden con el documento §46).
- PK: `id BIGINT UNSIGNED AUTO_INCREMENT`.
- FK: `<tabla_singular>_id`.
- Fechas: `DATETIME(6)`. `created_at` y `updated_at` en todo lo mutable.
- Importes: `DECIMAL(12,2)`. **Nunca** `FLOAT` ni `DOUBLE`.
- Estados: `VARCHAR` + `CHECK`, mapeado a `enum` de Java con `@Enumerated(STRING)`.
  Se evita el tipo `ENUM` de MySQL porque añadir un valor exige `ALTER TABLE`.
- Borrado lógico mediante columna `status`; **no** se borra físicamente nada con
  valor histórico.

---

## 3. Seguridad y acceso

### `users`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT UNSIGNED | PK |
| username | VARCHAR(50) | **UNIQUE**, NOT NULL |
| email | VARCHAR(120) | UNIQUE, NULL |
| password_hash | VARCHAR(100) | NOT NULL |
| full_name | VARCHAR(120) | NOT NULL |
| dni | VARCHAR(15) | UNIQUE, NULL |
| phone | VARCHAR(20) | NULL |
| status | VARCHAR(20) | NOT NULL, DEFAULT `ACTIVE` · ACTIVE/INACTIVE/BLOCKED |
| failed_attempts | SMALLINT | NOT NULL DEFAULT 0 |
| locked_until | DATETIME(6) | NULL |
| must_change_password | BOOLEAN | NOT NULL DEFAULT FALSE |
| last_login_at | DATETIME(6) | NULL |
| created_at / updated_at | DATETIME(6) | NOT NULL |

### `roles`
`id` · `code` **UNIQUE** (`ADMINISTRADOR`, `SUPERVISOR`, `VENDEDOR`, `ALMACENERO`) ·
`name` · `description` · `is_system` BOOLEAN — los roles de sistema no se pueden borrar.

### `permissions`
`id` · `code` **UNIQUE** (`VENTAS_ANULAR`) · `module` (`VENTAS`) · `description`.
La columna `module` permite agrupar los permisos en la pantalla de roles.

### `role_permissions`
PK compuesta `(role_id, permission_id)`. Ambas FK con `ON DELETE CASCADE`.

### `user_roles`
PK compuesta `(user_id, role_id)`. Un usuario puede acumular varios roles; sus
permisos efectivos son la **unión** de los permisos de todos sus roles.

### `refresh_tokens`
`id` · `user_id` FK · `token_hash` **UNIQUE** · `expires_at` · `revoked_at` ·
`created_at`. Se guarda el **hash** del token, no el token, para que una fuga de
la base de datos no permita suplantar sesiones.

---

## 4. Catálogo

### `categories`
`id` · `name` **UNIQUE** · `slug` UNIQUE · `status` · timestamps.

### `subcategories`
`id` · `category_id` FK → categories · `name` · `slug` · `status`.
**UNIQUE (category_id, name)** — "Manga corta" puede existir en Polos y en Camisas.

### `brands`
`id` · `name` **UNIQUE** · `status`.

### `colors`
`id` · `name` **UNIQUE** · `hex_code` CHAR(7) — permite pintar el color real en la
UI en lugar de mostrar solo texto · `status`.

### `sizes`
`id` · `name` **UNIQUE** · `sort_order` SMALLINT · `status`.
`sort_order` es necesario porque el orden natural de tallas (XS,S,M,L,XL,XXL) no
es alfabético.

---

## 5. Productos y variantes

### `products`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT UNSIGNED | PK |
| internal_code | VARCHAR(30) | **UNIQUE**, NOT NULL |
| sku | VARCHAR(40) | **UNIQUE**, NOT NULL |
| name | VARCHAR(150) | NOT NULL, INDEX (búsqueda) |
| category_id | BIGINT | FK NOT NULL |
| subcategory_id | BIGINT | FK NULL |
| brand_id | BIGINT | FK NULL |
| description | TEXT | NULL |
| price | DECIMAL(12,2) | NOT NULL, `CHECK (price >= 0)` |
| promo_price | DECIMAL(12,2) | NULL, `CHECK (promo_price >= 0)` |
| status | VARCHAR(20) | NOT NULL DEFAULT `ACTIVE` |
| image_url | VARCHAR(255) | NULL |
| material / fit | VARCHAR(150) / VARCHAR(100) | NULL (desde V21) — texto libre para la ficha de la tienda online |
| size_guide_image_url | VARCHAR(255) | NULL (desde V21) — imagen de guía de tallas de la tienda online |
| created_at / updated_at | DATETIME(6) | NOT NULL |
| created_by / updated_by | BIGINT | FK → users |

Índices: `idx_products_name(name)`, `idx_products_category(category_id)`,
`idx_products_status(status)`.

### `product_variants`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT UNSIGNED | PK |
| product_id | BIGINT | FK NOT NULL |
| color_id | BIGINT | FK NOT NULL |
| size_id | BIGINT | FK NOT NULL |
| sku | VARCHAR(60) | **UNIQUE**, NOT NULL |
| barcode | VARCHAR(20) | **UNIQUE**, NULL |
| stock | INT | NOT NULL DEFAULT 0, `CHECK (stock >= 0)` |
| min_stock | INT | NOT NULL DEFAULT 0, `CHECK (min_stock >= 0)` |
| status | VARCHAR(20) | NOT NULL DEFAULT `ACTIVE` |
| created_at / updated_at | DATETIME(6) | NOT NULL |

**Restricciones clave:**
- `uk_variant_combination UNIQUE (product_id, color_id, size_id)` — impide
  registrar dos veces "Polo Oversize / Negro / M".
- `uk_variant_barcode UNIQUE (barcode)` — el código de barras es único en todo el
  sistema. Es `NULL` mientras no se le asigne (MySQL permite varios `NULL` en un
  índice único, que es justo el comportamiento que se necesita).
- `CHECK (stock >= 0)` — última línea de defensa contra la sobreventa, además de
  la validación en el service.

**SKU vs código de barras** — son campos distintos y nunca intercambiables:

| | SKU | Código de barras |
|---|---|---|
| Para qué | identificador interno legible | lectura con pistola |
| Ejemplo | `POL-00125-M-NEG` | `7750000001255` |
| Formato | definido por la empresa | EAN-13 con dígito verificador |
| Lo usa | personal, reportes | escáner en POS |

---

## 6. Inventario

### `branches` · `warehouses`
`branches`: `id` · `code` UNIQUE · `name` · `address` · `phone` · `status`.
`warehouses`: `id` · `branch_id` FK · `code` UNIQUE · `name` · `status`.

En Fase 1 hay un registro de cada uno (`Tienda Principal` / `Almacén Principal`).
Existen desde el inicio para que los movimientos ya nazcan con `warehouse_id` y
el salto a multisucursal no exija reescribir el histórico.

### `inventory_movements` — **inmutable**
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT UNSIGNED | PK |
| variant_id | BIGINT | FK NOT NULL |
| warehouse_id | BIGINT | FK NOT NULL |
| type | VARCHAR(20) | NOT NULL · ENTRADA/SALIDA/VENTA/DEVOLUCION/AJUSTE/MERMA |
| quantity | INT | NOT NULL, `CHECK (quantity <> 0)` — **con signo** |
| stock_before | INT | NOT NULL |
| stock_after | INT | NOT NULL |
| reference_type | VARCHAR(20) | NULL · SALE/RETURN/ADJUSTMENT |
| reference_id | BIGINT | NULL |
| reason | VARCHAR(255) | NULL |
| user_id | BIGINT | FK NOT NULL |
| created_at | DATETIME(6) | NOT NULL |

No tiene `updated_at` **a propósito**: un movimiento nunca se modifica. Corregir
un error de inventario se hace con un movimiento `AJUSTE` en sentido contrario,
igual que en contabilidad.

`quantity` lleva signo (`-1` en una venta, `+10` en una entrada). Así se cumple
siempre `stock_after = stock_before + quantity`, y la suma de todos los
movimientos de una variante debe ser igual a su columna `stock`: es una
invariante verificable con una sola consulta.

Índices: `idx_mov_variant(variant_id, created_at)`, `idx_mov_type(type)`,
`idx_mov_reference(reference_type, reference_id)`.

---

## 7. Clientes

### `customers`
`id` · `full_name` NOT NULL · `doc_type` (DNI/RUC/CE/SIN_DOCUMENTO) ·
`doc_number` **UNIQUE NULL** · `phone` · `email` **UNIQUE NULL** (desde Fase 2) ·
`password_hash` VARCHAR(100) NULL (desde Fase 2) · `birth_date` · `status` ·
timestamps.

DNI y email son opcionales según el requisito. El historial de compras y los
totales acumulados **no se guardan como columnas**: se calculan consultando
`sales`, evitando datos redundantes que puedan desincronizarse.

`password_hash` es NULL salvo que el cliente se haya registrado en la tienda
online (§14). Se reutiliza la misma tabla en vez de crear un modelo de cuenta
aparte: si alguien se registra con un correo que ya existe (un cliente que
compró antes en tienda física, sin contraseña), se le asigna la contraseña a
ese mismo registro — así su historial de compras físicas queda ligado a su
cuenta online. `email` pasa a ser **UNIQUE** con la migración V18 para que esa
búsqueda por correo sea inequívoca.

---

## 8. Ventas

### `sales`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT UNSIGNED | PK |
| sale_number | VARCHAR(20) | **UNIQUE**, NOT NULL · `V001-00000123` |
| customer_id | BIGINT | FK NULL |
| user_id | BIGINT | FK NOT NULL — vendedor (quien opera la caja, el que figura en el ticket) |
| promoter_id | BIGINT | FK NULL — promotor de piso que ofreció la prenda, si hubo uno (opcional, solo para comisión/reportes; **nunca aparece en el ticket**) |
| cash_session_id | BIGINT | FK NOT NULL |
| subtotal | DECIMAL(12,2) | NOT NULL |
| discount_amount | DECIMAL(12,2) | NOT NULL DEFAULT 0 |
| total | DECIMAL(12,2) | NOT NULL, `CHECK (total >= 0)` |
| status | VARCHAR(25) | NOT NULL · COMPLETED/CANCELLED/RETURNED/PARTIALLY_RETURNED |
| notes | VARCHAR(255) | NULL |
| created_at | DATETIME(6) | NOT NULL |
| cancelled_at | DATETIME(6) | NULL |
| cancelled_by | BIGINT | FK NULL |
| cancellation_reason | VARCHAR(255) | NULL |
| authorized_by | BIGINT | FK NULL — quién autorizó la anulación |

`cash_session_id` es obligatorio: **no se puede vender sin caja abierta**, y así
el arqueo siempre cuadra.

Índices: `idx_sales_created(created_at)`, `idx_sales_user(user_id)`,
`idx_sales_customer(customer_id)`, `idx_sales_status(status)`.

### `sale_details`
`id` · `sale_id` FK · `variant_id` FK · `quantity` `CHECK (> 0)` ·
`unit_price` · `discount_amount` · `subtotal` ·
**snapshot:** `product_name`, `variant_sku`, `color_name`, `size_name`.

El snapshot es deliberado (decisión D-05): si mañana renombran el producto o
cambian su precio, la venta histórica sigue mostrando lo que realmente se vendió
ese día y por cuánto. Sin él, los reportes del pasado cambiarían solos.

### `payment_methods`
| Columna | Uso |
|---|---|
| code **UNIQUE** | EFECTIVO, YAPE, PLIN, TARJETA, TRANSFERENCIA |
| type | CASH / DIGITAL_WALLET / CARD / TRANSFER |
| **affects_cash** | BOOLEAN — solo `true` en efectivo |
| requires_reference | BOOLEAN — pide número de operación |
| account_holder / account_number | datos oficiales de Yape/Plin |
| qr_image_url | QR mostrado en el POS |
| status · sort_order | |

`affects_cash` es la columna que decide qué entra en el arqueo de caja: un cobro
por Yape no aumenta el efectivo del cajón. Los datos de cuenta solo los edita
quien tenga `CONFIGURACION_PAGOS` — nunca desde el POS (regla 8).

### `payments`
`id` · `sale_id` FK · `payment_method_id` FK · `amount` `CHECK (> 0)` ·
`reference` (nº de operación) · `status` (PENDING/COMPLETED/REFUNDED) · `created_at`.

Varias filas por venta ⇒ **pago mixto**. La invariante
`SUM(payments.amount) = sales.total` se valida en el service dentro de la
transacción.

### `promoters`
`id` · `name` · `status` (ACTIVE/INACTIVE) · `created_at` · `updated_at`.

Personal de piso que ofrece la prenda pero no opera la caja. **No es un
usuario del sistema** (sin login, sin contraseña) — es solo un nombre
seleccionable, opcional, al momento de cobrar. Sirve para medir comisión con
el reporte "ventas por promotor" (conteo + total por promotor). Deliberadamente
separado de `users`: crear una cuenta con contraseña para alguien que nunca va
a loguearse no tenía sentido.

---

## 9. Caja

### `cash_registers`
`id` · `branch_id` FK · `code` UNIQUE · `name` · `status`.

### `cash_sessions`
| Columna | Tipo |
|---|---|
| id · cash_register_id FK · opened_by FK | |
| opening_amount | DECIMAL(12,2) NOT NULL |
| opened_at | DATETIME(6) NOT NULL |
| expected_amount / counted_amount / difference | DECIMAL(12,2) NULL |
| closed_by | FK NULL |
| closed_at | DATETIME(6) NULL |
| status | OPEN / CLOSED |
| notes | VARCHAR(255) |

**Una sola sesión abierta por caja** se garantiza en la propia base de datos con
una columna generada más un índice único:

```sql
open_register_id BIGINT UNSIGNED GENERATED ALWAYS AS
    (CASE WHEN status = 'OPEN' THEN cash_register_id END) VIRTUAL,
UNIQUE KEY uk_one_open_session (open_register_id)
```

Al haber múltiples `NULL` permitidos en un índice único, las sesiones cerradas no
estorban, pero dos sesiones abiertas en la misma caja son imposibles incluso ante
una condición de carrera.

`difference = counted_amount − expected_amount`. Se guarda calculada porque es un
dato contable que debe quedar congelado en el momento del cierre.

### `cash_movements` — inmutable
`id` · `cash_session_id` FK · `type` (APERTURA/VENTA/INGRESO/GASTO/RETIRO/DEVOLUCION) ·
`amount` **con signo** · `reference_type` · `reference_id` · `reason` ·
`user_id` FK · `created_at`.

Efectivo esperado al cierre = `opening_amount + SUM(cash_movements.amount)`.

---

## 10. Devoluciones

### `returns`
`id` · `return_number` **UNIQUE** · `sale_id` FK · `user_id` FK ·
`authorized_by` FK NULL · `total_amount` · `refund_method_id` FK ·
`reason` NOT NULL · `status` · `created_at`.

### `return_details`
`id` · `return_id` FK · `sale_detail_id` FK · `variant_id` FK ·
`quantity` `CHECK (> 0)` · `unit_price` · `subtotal` ·
**`restock` BOOLEAN NOT NULL** — decide explícitamente si la prenda vuelve al
stock. Una prenda dañada se devuelve al cliente pero no vuelve a la venta.

`sale_detail_id` permite validar que no se devuelva más cantidad de la vendida
en esa línea concreta.

---

## 11. Auditoría y configuración

### `audit_logs`
`id` · `user_id` FK NULL · `username` (**snapshot**, sobrevive al borrado del
usuario) · `action` · `entity` · `entity_id` · `old_value` JSON · `new_value` JSON ·
`result` (SUCCESS/DENIED/FAILURE) · `ip_address` VARCHAR(45) (cabe IPv6) ·
`user_agent` · `created_at`.

Índices: `idx_audit_user(user_id, created_at)`, `idx_audit_entity(entity, entity_id)`,
`idx_audit_created(created_at)`.

### `company_settings`
Fila única (`id = 1`): `name` · `ruc` · `address` · `phone` · `email` ·
`logo_url` · `currency_code` (PEN) · `currency_symbol` (S/) · `igv_rate` ·
`ticket_footer` · `shipping_flat_rate` (desde V19, §12) · `updated_at` · `updated_by`.

### `sequences`
`name` PK · `prefix` · `current_value` · `padding`.

Genera `sale_number`, `return_number`, SKU y correlativos de código de barras.
Se lee con `SELECT ... FOR UPDATE` para que dos cajas simultáneas no obtengan el
mismo número.

---

## 12. Tienda online (Fase 2 — catálogo + carrito + pedido)

Todo lo público vive en el paquete `tienda`, separado de `producto`/`cliente`,
para que quede auditable de un vistazo qué endpoints son intencionalmente
públicos. Ver docs/05-api.md §Tienda pública para los endpoints.

### `customer_refresh_tokens`
Espejo de `refresh_tokens`, pero para clientes: `id` · `customer_id` FK
(`ON DELETE CASCADE`) · `token_hash` **UNIQUE** · `expires_at` · `revoked_at` ·
`created_at`. Tabla separada de `refresh_tokens` (que es de staff) — un token
de cliente lleva `ROLE_CUSTOMER` como única autoridad en el JWT y nunca debe
poder confundirse con la sesión de un `Usuario`.

### `orders`
| Columna | Tipo | Restricciones |
|---|---|---|
| id | BIGINT UNSIGNED | PK |
| order_number | VARCHAR(20) | **UNIQUE**, NOT NULL · `PED00000123` |
| customer_id | BIGINT | FK NOT NULL |
| department / province | VARCHAR(100) | NOT NULL (desde V19) — junto con `district`, ubicación completa a nivel de todo el Perú |
| status | VARCHAR(20) | NOT NULL DEFAULT `PENDING_PAYMENT` · PENDING_PAYMENT/CONFIRMED/CANCELLED |
| subtotal | DECIMAL(12,2) | NOT NULL |
| shipping_cost | DECIMAL(12,2) | NOT NULL DEFAULT 0 (desde V19) |
| total | DECIMAL(12,2) | NOT NULL, `CHECK (total >= 0)` · `= subtotal + shipping_cost` |
| payment_method_id | BIGINT | FK NOT NULL |
| payment_reference | VARCHAR(50) | NULL |
| payment_proof_url | VARCHAR(255) | NULL (desde V19) — comprobante subido por el cliente |
| recipient_dni / recipient_first_name / recipient_last_name_paterno / recipient_last_name_materno | VARCHAR | NOT NULL (desde V20) — la courier (Shalom) exige DNI y apellidos separados del destinatario, no un nombre libre |
| phone / address / district | VARCHAR | NOT NULL — datos de entrega |
| notes | VARCHAR(255) | NULL |
| created_at | DATETIME(6) | NOT NULL |
| confirmed_at | DATETIME(6) | NULL |
| confirmed_by | BIGINT | FK → users, NULL |
| cancelled_at | DATETIME(6) | NULL |
| cancellation_reason | VARCHAR(255) | NULL |

**Por qué `orders` no es `sales`:** `sales.user_id` y `sales.cash_session_id`
son `NOT NULL` (toda venta exige un cajero y una caja abierta); un pedido web
no tiene ninguno de los dos al crearse. Forzar esos campos con un usuario o
una caja ficticios habría sido peor que tener dos tablas con un flujo de
estados distinto.

**Por qué el stock no se descuenta al crear el pedido:** el pago es manual
(el cliente elige Yape/Plin/transferencia y el staff lo confirma a mano), así
que "pedido creado" no significa "pago recibido". El stock recién se
descuenta cuando el staff confirma el pago (`InventarioService.registrarPorPedido`,
mismo mecanismo que una venta pero con `reference_type = 'ORDER'`), y se
reingresa si un pedido confirmado se cancela después
(`registrarPorCancelacionPedido`). Contrapartida asumida: dos clientes podrían
pedir la última unidad antes de que el staff confirme el primero — aceptable
para un MVP con confirmación manual, ya que el staff revisa cada pedido a
mano de todas formas.

### `order_details`
`id` · `order_id` FK · `variant_id` FK · `quantity` `CHECK (> 0)` ·
`unit_price` · `subtotal` ·
**snapshot:** `product_name`, `variant_sku`, `color_name`, `size_name`.

Mismo patrón de snapshot que `sale_details` (decisión D-05, §8).

`inventory_movements.reference_type` gana el valor `ORDER` (además de
SALE/RETURN/ADJUSTMENT) para que el movimiento de stock al confirmar un
pedido pueda referenciarlo.

### Envío y contraentrega (V19)

`company_settings` gana `shipping_flat_rate` (tarifa única, editable desde
Configuración) — se investigó a Shalom (la courier que usa la empresa) y no
tiene una API pública de tarifas: cobra por peso/tamaño y destino con
cotización manual, así que el monto lo define el staff en vez de calcularse
contra un servicio externo.

Se siembra un método de pago `CONTRAENTREGA` (`type = CASH`) en
`payment_methods`, exclusivo para el distrito de Huacho (sede física de la
empresa): el pedido se paga en efectivo al recibirlo y el envío es gratis
(`shipping_cost = 0`, se asume recojo/entrega local propia del negocio, no un
envío por courier). `PedidoService.crear()` valida el distrito exacto tanto
si el frontend lo permitió como si alguien intenta forzarlo por API
directamente.

Departamento/provincia/distrito se arman en el frontend a partir de un
dataset público de ubigeo (`joseluisq/ubigeos-peru`) embebido como JSON
estático — no se valida contra una tabla en el backend (son strings simples,
igual que `address`), la consistencia la da que el frontend arma el pedido
desde selects en cascada, no texto libre.

---

## 13. Redundancias evaluadas y descartadas

| Dato candidato | Decisión | Motivo |
|---|---|---|
| `customers.total_comprado` | **Descartado** | Se calcula desde `sales`; una columna se desincroniza en cuanto haya una anulación |
| `products.stock_total` | **Descartado** | Se suma desde `product_variants` |
| `sales.payment_method` | **Descartado** | Rompe el pago mixto; los métodos están en `payments` |
| `product_variants.stock` | **Conservado** | Redundante frente a los movimientos, pero necesario para la velocidad del POS. Se actualiza en la misma transacción y es verificable |
| Snapshot en `sale_details` | **Conservado** | No es redundancia: es una foto histórica que debe ser inmune a cambios posteriores |

## 14. Orden de creación de las migraciones

```
V1  → esquema de seguridad (users, roles, permissions, tablas puente, refresh_tokens)
V2  → estructura (branches, warehouses, cash_registers, sequences, company_settings)
V3  → catálogo (categories, subcategories, brands, colors, sizes)
V4  → productos y variantes
V5  → inventario (inventory_movements)
V6  → clientes
V7  → caja (cash_sessions, cash_movements)
V8  → ventas (sales, sale_details, payment_methods, payments)
V9  → devoluciones (returns, return_details)
V10 → auditoría (audit_logs)
V11 → datos semilla: permisos, roles, admin inicial, catálogos, métodos de pago
V12 → datos de demostración (perfil dev únicamente)
...
V18 → tienda online: customers.password_hash, customer_refresh_tokens, orders,
      order_details, reference_type ORDER, permisos PEDIDOS_*
V19 → envío: company_settings.shipping_flat_rate, orders.department/province/
      shipping_cost/payment_proof_url, método de pago CONTRAENTREGA
V20 → orders.recipient_dni/recipient_first_name/recipient_last_name_paterno/
      recipient_last_name_materno (reemplaza recipient_name)
V21 → products.material/fit/size_guide_image_url (ficha de tienda online)
```

(Lista ilustrativa de las primeras fases — el detalle exacto de cada migración
vive en `src/main/resources/db/migration/`, que es la fuente de verdad.)
