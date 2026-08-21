# 05 — API REST

**Base:** `/api` · **Formato:** JSON · **Auth:** `Authorization: Bearer <token>`
**Fechas:** ISO-8601 con zona (`2026-08-19T18:32:11-05:00`)
**Importes:** número con 2 decimales (`49.90`)

---

## 1. Convenciones

### Códigos de estado

| Código | Uso |
|---|---|
| 200 | Consulta o actualización correcta |
| 201 | Recurso creado (incluye `Location`) |
| 204 | Operación sin contenido de respuesta |
| 400 | Petición malformada o validación fallida |
| 401 | Sin autenticar o token expirado |
| 403 | Autenticado pero sin permiso |
| 404 | Recurso inexistente |
| 409 | Conflicto: duplicado o regla de negocio violada |
| 422 | Semánticamente incorrecto |
| 500 | Error interno |

### Respuesta de error (uniforme)

```json
{
  "timestamp": "2026-08-19T18:32:11-05:00",
  "status": 409,
  "error": "DUPLICATE_RESOURCE",
  "message": "El código de barras 7750000001255 ya está registrado",
  "path": "/api/variants",
  "fieldErrors": [
    { "field": "barcode", "message": "ya está registrado" }
  ]
}
```

### Paginación

Petición: `?page=0&size=20&sort=createdAt,desc`

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 143,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

---

## 2. Autenticación — `/api/auth`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/login` | público | Devuelve access + refresh token |
| POST | `/refresh` | público | Renueva el access token |
| POST | `/logout` | autenticado | Revoca el refresh token |
| GET | `/me` | autenticado | Usuario actual con sus permisos |
| POST | `/change-password` | autenticado | Cambio de contraseña propia |

**POST `/api/auth/login`**
```json
{ "username": "admin", "password": "..." }
```
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "9f2c...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "user": {
    "id": 1,
    "username": "admin",
    "fullName": "Administrador",
    "mustChangePassword": false,
    "roles": ["ADMINISTRADOR"],
    "permissions": ["VENTAS_CREAR", "VENTAS_ANULAR", "..."]
  }
}
```

El frontend usa `permissions` para ocultar acciones, pero **el backend vuelve a
comprobarlas siempre** (ocultar no es proteger).

---

## 3. Usuarios, roles y permisos

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/users` | `USUARIOS_CONSULTAR` |
| GET | `/api/users/{id}` | `USUARIOS_CONSULTAR` |
| POST | `/api/users` | `USUARIOS_CREAR` |
| PUT | `/api/users/{id}` | `USUARIOS_EDITAR` |
| PATCH | `/api/users/{id}/status` | `USUARIOS_BLOQUEAR` |
| POST | `/api/users/{id}/reset-password` | `USUARIOS_EDITAR` |
| GET | `/api/roles` | `ROLES_GESTIONAR` |
| POST | `/api/roles` | `ROLES_GESTIONAR` |
| PUT | `/api/roles/{id}` | `ROLES_GESTIONAR` |
| PUT | `/api/roles/{id}/permissions` | `ROLES_GESTIONAR` |
| GET | `/api/permissions` | `ROLES_GESTIONAR` |

No existe `DELETE /api/users/{id}`: un usuario se desactiva, porque sus ventas y
movimientos deben seguir siendo atribuibles.

---

## 4. Catálogo

Mismo patrón para `/api/categories`, `/api/subcategories`, `/api/brands`,
`/api/colors`, `/api/sizes`:

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/{recurso}` | `PRODUCTOS_CONSULTAR` |
| POST | `/api/{recurso}` | `CONFIGURACION_EDITAR` |
| PUT | `/api/{recurso}/{id}` | `CONFIGURACION_EDITAR` |
| PATCH | `/api/{recurso}/{id}/status` | `CONFIGURACION_EDITAR` |

`GET /api/subcategories?categoryId=3` filtra por categoría.

---

## 5. Productos — `/api/products`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/products` | `PRODUCTOS_CONSULTAR` | Listado paginado con filtros |
| GET | `/api/products/{id}` | `PRODUCTOS_CONSULTAR` | Detalle con variantes |
| POST | `/api/products` | `PRODUCTOS_CREAR` | |
| PUT | `/api/products/{id}` | `PRODUCTOS_EDITAR` | |
| PATCH | `/api/products/{id}/status` | `PRODUCTOS_EDITAR` | Activar / desactivar |
| POST | `/api/products/{id}/image` | `PRODUCTOS_EDITAR` | Subir imagen |
| POST | `/api/products/{id}/size-guide` | `PRODUCTOS_EDITAR` | Subir imagen de guía de tallas (tienda online) |

`material` y `fit` (texto libre, ej. "100% Algodón" / "True to size") viajan
en `CrearProductoRequest`/`ActualizarProductoRequest` y se muestran en la
ficha pública del producto.

**Filtros:** `?search=polo&categoryId=1&subcategoryId=4&brandId=2&status=ACTIVE&minPrice=20&maxPrice=100`

**POST `/api/products`**
```json
{
  "internalCode": "POL-0012",
  "sku": "POL-00125",
  "name": "Polo Oversize",
  "categoryId": 1,
  "subcategoryId": 3,
  "brandId": 2,
  "description": "Polo oversize de algodón peruano 100%",
  "price": 49.90,
  "promoPrice": 39.90
}
```

Si se omite `sku`, el backend lo genera desde la tabla de secuencias.

---

## 6. Variantes — `/api/variants`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/products/{id}/variants` | `PRODUCTOS_CONSULTAR` | Variantes del producto |
| POST | `/api/products/{id}/variants` | `VARIANTES_GESTIONAR` | Crear una |
| POST | `/api/products/{id}/variants/bulk` | `VARIANTES_GESTIONAR` | **Generar la matriz color × talla** |
| PUT | `/api/variants/{id}` | `VARIANTES_GESTIONAR` | |
| PATCH | `/api/variants/{id}/status` | `VARIANTES_GESTIONAR` | |
| GET | `/api/variants/barcode/{barcode}` | `PRODUCTOS_CONSULTAR` | **Búsqueda por escaneo** |
| GET | `/api/variants/search?q=` | `PRODUCTOS_CONSULTAR` | Búsqueda para POS |

**`bulk`** resuelve el caso real: dar de alta un polo en 2 colores × 6 tallas son
12 variantes que nadie quiere crear a mano.
```json
{ "colorIds": [1, 2], "sizeIds": [1,2,3,4,5,6], "minStock": 3, "generateBarcodes": true }
```
Crea las combinaciones que falten y omite las que ya existan, sin fallar.

**`GET /api/variants/barcode/{barcode}`** es el endpoint más crítico del POS.
Responde en una sola consulta todo lo que necesita la pantalla de venta:
```json
{
  "variantId": 145,
  "productName": "Polo Oversize",
  "colorName": "Negro",
  "sizeName": "M",
  "sku": "POL-00125-M-NEG",
  "barcode": "7750000001255",
  "price": 49.90,
  "promoPrice": 39.90,
  "effectivePrice": 39.90,
  "stock": 12,
  "status": "ACTIVE"
}
```

---

## 7. Códigos de barras — `/api/barcodes`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/api/barcodes/generate` | `BARCODE_GENERAR` | Genera EAN-13 sin asignar |
| POST | `/api/variants/{id}/barcode` | `BARCODE_GENERAR` | Asigna o regenera |
| GET | `/api/barcodes/{code}/validate` | `PRODUCTOS_CONSULTAR` | Comprueba disponibilidad |
| GET | `/api/variants/{id}/label` | `BARCODE_GENERAR` | Etiqueta imprimible (PNG/PDF) |
| POST | `/api/barcodes/labels` | `BARCODE_GENERAR` | Lote de etiquetas |

Los códigos generados son **EAN-13 válidos** con prefijo interno `775` y dígito
verificador calculado, para que cualquier lector estándar los acepte.

---

## 8. Inventario — `/api/inventory`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/inventory` | `INVENTARIO_CONSULTAR` | Stock por variante |
| GET | `/api/inventory/low-stock` | `INVENTARIO_CONSULTAR` | `stock <= min_stock` |
| GET | `/api/inventory/out-of-stock` | `INVENTARIO_CONSULTAR` | `stock = 0` |
| GET | `/api/inventory/movements` | `INVENTARIO_CONSULTAR` | Historial con filtros |
| GET | `/api/inventory/movements?variantId=` | `INVENTARIO_CONSULTAR` | Historial de una variante |
| POST | `/api/inventory/entry` | `INVENTARIO_ENTRADA` | Entrada |
| POST | `/api/inventory/exit` | `INVENTARIO_SALIDA` | Salida |
| POST | `/api/inventory/adjustment` | `INVENTARIO_AJUSTAR` | Ajuste con motivo obligatorio |

**No hay `PUT` ni `DELETE` sobre movimientos: son inmutables (RN-06).**

```json
// POST /api/inventory/adjustment
{ "variantId": 145, "newStock": 18, "reason": "Recuento físico del 19/08" }
```
El backend calcula la diferencia y crea un movimiento `AJUSTE` con esa cantidad.

---

## 9. Clientes — `/api/customers`

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/customers` | `CLIENTES_CONSULTAR` |
| GET | `/api/customers/{id}` | `CLIENTES_CONSULTAR` |
| GET | `/api/customers/search?q=` | `CLIENTES_CONSULTAR` |
| GET | `/api/customers/{id}/purchases` | `CLIENTES_CONSULTAR` |
| POST | `/api/customers` | `CLIENTES_CREAR` |
| PUT | `/api/customers/{id}` | `CLIENTES_EDITAR` |

`/purchases` devuelve el historial más los agregados (total comprado, número de
compras, última compra, productos comprados) calculados en consulta.

---

## 10. Ventas — `/api/sales`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/sales` | `VENTAS_CONSULTAR` | Listado con filtros |
| GET | `/api/sales/{id}` | `VENTAS_CONSULTAR` | Detalle completo |
| GET | `/api/sales/{id}/ticket` | `VENTAS_CONSULTAR` | Ticket imprimible |
| POST | `/api/sales` | `VENTAS_CREAR` | **Registrar venta** |
| POST | `/api/sales/{id}/cancel` | `VENTAS_ANULAR` | Anular con motivo |

Un vendedor sin `VENTAS_CONSULTAR` global solo ve sus propias ventas: el filtro
lo aplica el service, no el cliente.

**POST `/api/sales`** — operación transaccional completa:
```json
{
  "customerId": 12,
  "promoterId": null,
  "cashSessionId": 4,
  "discountAmount": 0.00,
  "notes": null,
  "items": [
    { "variantId": 145, "quantity": 2, "unitPrice": 39.90, "discountAmount": 0.00 },
    { "variantId": 201, "quantity": 1, "unitPrice": 89.90, "discountAmount": 10.00 }
  ],
  "payments": [
    { "paymentMethodId": 1, "amount": 100.00, "reference": null },
    { "paymentMethodId": 2, "amount": 59.80, "reference": "OP-88213" }
  ]
}
```

`201 Created`:
```json
{
  "id": 1523,
  "saleNumber": "V001-00001523",
  "subtotal": 169.70,
  "discountAmount": 10.00,
  "total": 159.80,
  "status": "COMPLETED",
  "createdAt": "2026-08-19T18:32:11-05:00",
  "seller": { "id": 3, "fullName": "Carlos Ramírez" },
  "customer": { "id": 12, "fullName": "María Quispe" },
  "items": [],
  "payments": []
}
```

Errores posibles: `409 INSUFFICIENT_STOCK`, `409 BUSINESS_RULE_VIOLATION`
(pagos ≠ total, sin caja abierta), `403 ACCESS_DENIED` (descuento sin permiso).

### Promotores — `/api/promoters`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/promoters` | `PROMOTORES_CONSULTAR` | Listado (para el select opcional del POS) |
| POST | `/api/promoters` | `PROMOTORES_GESTIONAR` | Crear |
| PUT | `/api/promoters/{id}` | `PROMOTORES_GESTIONAR` | Editar nombre |
| PATCH | `/api/promoters/{id}/status` | `PROMOTORES_GESTIONAR` | Activar/desactivar |

Un promotor **no es un usuario del sistema**: sin login, sin contraseña, solo
un nombre. `promoterId` en `POST /api/sales` es opcional — se deja `null`
cuando nadie de piso ofreció la prenda. Nunca se imprime en el ticket, solo
sirve para el reporte de comisión (`/api/reports/sales/by-promoter`).

---

## 11. Pagos y métodos — `/api/payment-methods`

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/payment-methods` | autenticado |
| GET | `/api/payment-methods/{id}` | autenticado |
| PUT | `/api/payment-methods/{id}` | `CONFIGURACION_PAGOS` |
| POST | `/api/payment-methods/{id}/qr` | `CONFIGURACION_PAGOS` |

El POS consume `GET` y muestra los datos oficiales en **solo lectura** (RN-15).
Los pagos no tienen endpoint propio: se crean como parte de la venta, para que no
puedan existir sueltos ni descuadrados.

---

## 12. Caja — `/api/cash-registers`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/cash-registers` | `CAJA_CONSULTAR` | Cajas físicas |
| GET | `/api/cash-registers/sessions` | `CAJA_CONSULTAR` | Historial de sesiones |
| GET | `/api/cash-registers/sessions/current` | `CAJA_CONSULTAR` | Sesión abierta del usuario |
| GET | `/api/cash-registers/sessions/{id}` | `CAJA_CONSULTAR` | Detalle con movimientos |
| POST | `/api/cash-registers/sessions` | `CAJA_ABRIR` | Abrir caja |
| POST | `/api/cash-registers/sessions/{id}/close` | `CAJA_CERRAR` | Cerrar con arqueo |
| GET | `/api/cash-registers/sessions/{id}/summary` | `CAJA_CONSULTAR` | Previsualizar el arqueo |
| POST | `/api/cash-registers/movements` | `CAJA_MOVIMIENTO` | Ingreso / gasto / retiro |

**Cierre** — el cajero envía solo lo que ha contado; el sistema calcula el resto:
```json
{ "countedAmount": 980.00, "notes": "Faltante detectado en el turno tarde" }
```
```json
{
  "expectedAmount": 1000.00,
  "countedAmount": 980.00,
  "difference": -20.00,
  "status": "CLOSED",
  "closedAt": "2026-08-19T21:05:00-05:00"
}
```

---

## 13. Devoluciones — `/api/returns`

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/returns` | `VENTAS_CONSULTAR` |
| GET | `/api/returns/{id}` | `VENTAS_CONSULTAR` |
| POST | `/api/returns` | `VENTAS_DEVOLVER` |
| GET | `/api/sales/{id}/returnable-items` | `VENTAS_DEVOLVER` |

`returnable-items` devuelve cuánto queda por devolver de cada línea, para que la
interfaz no permita superar lo vendido.

```json
// POST /api/returns
{
  "saleId": 1523,
  "reason": "Talla incorrecta",
  "refundMethodId": 1,
  "items": [
    { "saleDetailId": 3011, "quantity": 1, "restock": true }
  ]
}
```

---

## 14. Reportes — `/api/reports`

Todos aceptan `?from=2026-08-01&to=2026-08-19` y requieren `REPORTES_CONSULTAR`.

| Ruta | Contenido |
|---|---|
| `/api/reports/dashboard` | Métricas del panel principal |
| `/api/reports/sales/summary` | Totales, ticket medio, nº de ventas |
| `/api/reports/sales/by-day` | Serie temporal |
| `/api/reports/sales/by-category` | Ventas por categoría |
| `/api/reports/sales/by-seller` | Ventas por vendedor |
| `/api/reports/sales/by-promoter` | Ventas por promotor (conteo + total, para comisión) |
| `/api/reports/sales/by-payment-method` | Distribución de cobros |
| `/api/reports/products/top-selling` | Más vendidos |
| `/api/reports/products/no-movement` | Sin rotación |
| `/api/reports/inventory/valuation` | Valorización del stock |
| `/api/reports/inventory/by-size` | Stock por talla |
| `/api/reports/inventory/by-color` | Stock por color |
| `/api/reports/cash/sessions` | Aperturas, cierres y diferencias |

`GET /api/reports/dashboard`:
```json
{
  "salesToday": { "count": 23, "total": 1847.50 },
  "salesMonth": { "count": 412, "total": 32180.00 },
  "productsSoldToday": 41,
  "lowStockCount": 7,
  "outOfStockCount": 2,
  "paymentBreakdown": [
    { "method": "EFECTIVO", "total": 820.00, "percentage": 44.4 },
    { "method": "YAPE", "total": 640.50, "percentage": 34.7 }
  ],
  "salesByDay": [],
  "topProducts": []
}
```

Exportación (`REPORTES_EXPORTAR`): `?format=xlsx|pdf|csv`.

---

## 15. Auditoría — `/api/audit`

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/audit` | `AUDITORIA_CONSULTAR` |
| GET | `/api/audit/{id}` | `AUDITORIA_CONSULTAR` |

Filtros: `?userId=&action=&entity=&result=&from=&to=`.
**Solo lectura**: no existe forma de escribir ni borrar auditoría por API.

---

## 16. Configuración — `/api/settings`

| Método | Ruta | Permiso |
|---|---|---|
| GET | `/api/settings/company` | `CONFIGURACION_VER` |
| PUT | `/api/settings/company` | `CONFIGURACION_EDITAR` |
| POST | `/api/settings/company/logo` | `CONFIGURACION_EDITAR` |

`shippingFlatRate` (desde Fase 2) es la tarifa de envío que se cobra en todo
pedido online salvo contraentrega en Huacho — se edita aquí, no hay un
servicio externo del que leerla (ver docs/03 §12).

---

## 17. Búsqueda global — `/api/search`

`GET /api/search?q=polo` — devuelve resultados agrupados por tipo (productos,
variantes, clientes, ventas). Previsto en el diseño, se implementa al final de la
Fase 2.

---

## 18. Pedidos (staff) — `/api/orders`

Vista de staff sobre los pedidos hechos desde la tienda online. Ver §19 para el
lado del cliente (`/api/store/orders`).

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/api/orders` | `PEDIDOS_CONSULTAR` | Listado con filtros `status`, `from`, `to` |
| GET | `/api/orders/{id}` | `PEDIDOS_CONSULTAR` | Detalle completo con items |
| POST | `/api/orders/{id}/confirm` | `PEDIDOS_GESTIONAR` | Confirma el pago — **descuenta stock** y pasa a `CONFIRMED` |
| POST | `/api/orders/{id}/cancel` | `PEDIDOS_GESTIONAR` | Anula — si estaba `CONFIRMED`, **reingresa stock** |

```json
// POST /api/orders/{id}/cancel
{ "reason": "El cliente no completó el pago a tiempo" }
```

Errores posibles: `409 BUSINESS_RULE_VIOLATION` (confirmar un pedido que no
está `PENDING_PAYMENT`, o anular uno ya `CANCELLED`), `409 INSUFFICIENT_STOCK`
(al confirmar, si el stock ya no alcanza — puede pasar si otro pedido o venta
se adelantó).

---

## 19. Tienda pública — `/api/store/**`

Todo bajo `/api/store` es intencionalmente distinto del resto de la API: el
catálogo es público (sin token) y la autenticación de clientes es un sistema
paralelo al de staff — un JWT de cliente lleva `ROLE_CUSTOMER` como única
autoridad y nunca sirve para pasar un `hasAuthority('VENTAS_...')` de staff,
ni viceversa. Ver docs/03-modelo-datos.md §12.

### Catálogo — `/api/store/catalog` (público, sin autenticación)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/store/catalog/products` | Paginado; filtros `search`, `categoryId`, `brandId`. Cada item incluye `colors: [{name, hexCode}]` (colores distintos con stock, máx. 6) para los swatches del listado |
| GET | `/api/store/catalog/products/{id}` | Detalle + variantes (color/talla) con `inStock` |
| GET | `/api/store/catalog/categories` | Solo categorías activas |
| GET | `/api/store/catalog/brands` | Solo marcas activas |
| GET | `/api/store/catalog/payment-methods` | Métodos activos, con QR/cuenta para pagar. Incluye `code` (ej. `CONTRAENTREGA`) para que el checkout decida cuándo mostrar cada opción |
| GET | `/api/store/catalog/shipping-info` | `{ flatRate, freeShippingDistrict }` — tarifa de envío vigente y el distrito con envío gratis (Huacho) |

Las respuestas usan DTOs propios (`Public*Response`) que **nunca** exponen
`internalCode`, SKU/barcode interno, ni `stock`/`minStock` exactos — solo
`inStock` booleano. Esto es deliberado: son datos operativos internos, no
información de cara al público.

```json
// GET /api/store/catalog/products/{id}
{
  "id": 1,
  "name": "Polo Oversize",
  "description": "Polo oversize de algodón peruano 100%",
  "material": "100% Algodón",
  "fit": "True to size",
  "price": 49.90,
  "promoPrice": 39.90,
  "imageUrl": "/uploads/products/....jpg",
  "sizeGuideImageUrl": "/uploads/size-guides/....jpg",
  "categoryName": "Polos",
  "brandName": null,
  "variants": [
    { "variantId": 145, "colorId": 3, "colorName": "Negro", "colorHex": "#000000", "sizeId": 2, "sizeName": "M", "inStock": true }
  ]
}
```

### Autenticación de clientes — `/api/store/auth`

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| POST | `/api/store/auth/register` | público | Crea cuenta o reclama un `Customer` existente sin contraseña por email |
| POST | `/api/store/auth/login` | público | |
| POST | `/api/store/auth/refresh` | público | Mismo formato que `/api/auth/refresh` |
| GET | `/api/store/auth/me` | `ROLE_CUSTOMER` | Datos del cliente autenticado |

```json
// POST /api/store/auth/register
{ "email": "maria@example.com", "password": "Maria2026", "fullName": "María Quispe", "phone": "987654321" }
```
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "9f2c...",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "customer": { "id": 12, "fullName": "María Quispe", "email": "maria@example.com", "phone": "987654321" }
}
```

Si ya existía un `Customer` con ese email (de una compra en tienda física,
sin contraseña), el registro **reclama ese mismo registro** en vez de crear
uno duplicado — así el historial de compras físicas queda ligado a la cuenta
online. `409 DUPLICATE_RESOURCE` si el email ya tiene contraseña.

### Pedidos del cliente — `/api/store/orders` (`ROLE_CUSTOMER`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/store/orders` | Crea el pedido — el precio se resuelve server-side, nunca viaja del cliente |
| GET | `/api/store/orders` | Pedidos propios, paginado |
| GET | `/api/store/orders/{id}` | Detalle propio (404 si no es del cliente autenticado) |
| POST | `/api/store/orders/{id}/payment-proof` | Multipart; sube/reemplaza el comprobante de pago (opcional, solo si `PENDING_PAYMENT`) |

```json
// POST /api/store/orders
{
  "items": [{ "variantId": 145, "quantity": 2 }],
  "paymentMethodId": 2,
  "paymentReference": null,
  "recipientDni": "45678912",
  "recipientFirstName": "María",
  "recipientLastNamePaterno": "Quispe",
  "recipientLastNameMaterno": "Ramos",
  "phone": "987654321",
  "address": "Av. Siempre Viva 123",
  "department": "Lima",
  "province": "Lima",
  "district": "San Isidro",
  "notes": null
}
```

`201 Created`, status inicial `PENDING_PAYMENT`. `total = subtotal + shippingCost`:
el envío es la tarifa plana vigente (`/api/store/catalog/shipping-info`),
salvo que el método de pago sea `CONTRAENTREGA` (solo permitido si
`district = "Huacho"`, validado también en el backend), en cuyo caso
`shippingCost = 0`. El stock **no** se descuenta en este paso — recién al
confirmar el pago desde `/api/orders/{id}/confirm` (§18). El endpoint sí
valida que haya stock suficiente en el momento de crear el pedido (chequeo
informativo, no una reserva).

```json
// POST /api/store/orders/{id}/payment-proof (multipart, campo "file")
```
Devuelve el pedido completo con `paymentProofUrl` actualizado. El staff lo ve
en `GET /api/orders/{id}` (§18) al revisar el pedido antes de confirmar.
