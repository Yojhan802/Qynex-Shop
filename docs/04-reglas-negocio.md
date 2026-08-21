# 04 — Reglas de negocio

Cada regla indica **dónde se aplica** y **qué error devuelve**. El backend es
siempre la autoridad final: aunque el frontend valide, el servidor vuelve a
validar.

---

## RN-01 · No se vende sin stock suficiente

**Dónde:** `VentaService.registrarVenta()`
**Cómo:** antes de descontar, se bloquean las variantes con `PESSIMISTIC_WRITE`
(ordenadas por id ascendente para evitar interbloqueos) y se comprueba
`stock >= cantidad`.
**Defensa adicional:** `CHECK (stock >= 0)` en la tabla.
**Error:** `409 INSUFFICIENT_STOCK` — *"Stock insuficiente para Polo Oversize Negro M. Disponible: 2, solicitado: 3"*

> El bloqueo pesimista es necesario: dos cajeros vendiendo la última unidad al
> mismo tiempo es un caso real en una tienda con varias cajas.

## RN-02 · Códigos de barras únicos

**Dónde:** `VarianteService` + restricción `uk_variant_barcode`.
**Cómo:** se comprueba antes de insertar y se captura además la violación de
integridad, por si dos peticiones concurrentes pasan la comprobación a la vez.
**Error:** `409 DUPLICATE_RESOURCE` — *"El código de barras 7750000001255 ya está registrado"*

## RN-03 · SKU únicos

**Política:** el SKU de producto es único globalmente; el SKU de variante también.
**Formato:** producto `POL-00125`; variante `POL-00125-M-NEG`
(`<sku_producto>-<talla>-<color_3_letras>`).
**Generación:** automática desde la tabla `sequences`, editable manualmente si el
resultado sigue siendo único.
**Error:** `409 DUPLICATE_RESOURCE`

## RN-04 · Una variante no se repite dentro del producto

**Dónde:** `uk_variant_combination UNIQUE (product_id, color_id, size_id)`.
**Error:** `409 DUPLICATE_RESOURCE` — *"Ya existe la variante Negro / M para este producto"*

## RN-05 · El stock nunca cambia sin movimiento

**Dónde:** `InventarioService` es el **único** componente autorizado a escribir la
columna `stock`. Ningún otro service la toca.
**Cómo:** todo cambio pasa por `registrarMovimiento()`, que en una sola operación
lee el stock actual, calcula el nuevo, guarda el movimiento con
`stock_before`/`stock_after` y actualiza la variante.
**Invariante verificable:**
```sql
SELECT v.id FROM product_variants v
JOIN (SELECT variant_id, SUM(quantity) s FROM inventory_movements GROUP BY variant_id) m
  ON m.variant_id = v.id
WHERE v.stock <> m.s;   -- debe devolver 0 filas siempre
```

## RN-06 · Los movimientos de inventario son inmutables

No existen endpoints `PUT` ni `DELETE` sobre `/api/inventory/movements`. La
entidad tiene los campos `@Column(updatable = false)`. Un error se corrige con un
movimiento `AJUSTE` en sentido contrario, con motivo obligatorio.

## RN-07 · La suma de pagos debe igualar el total

**Dónde:** `VentaService`, antes de confirmar.
**Cómo:** `sum(pagos) == total` comparando `BigDecimal` con `compareTo` y escala 2.
**No se implementa venta a crédito** en esta fase: un pago parcial es un error.
**Error:** `409 BUSINESS_RULE_VIOLATION` — *"La suma de pagos (S/70.00) no coincide con el total (S/80.00)"*

Ejemplo válido de pago mixto: total S/80 = efectivo S/50 + Yape S/30.

## RN-08 · No se vende sin caja abierta

**Dónde:** `VentaService`. La venta exige una `cash_session` en estado `OPEN`
para la caja del vendedor.
**Error:** `409 BUSINESS_RULE_VIOLATION` — *"No hay una sesión de caja abierta"*

## RN-09 · Solo una sesión abierta por caja

**Dónde:** garantizado por la base de datos con la columna generada
`open_register_id` y su índice único (ver [03-modelo-datos.md](03-modelo-datos.md) §9).
**Error:** `409 BUSINESS_RULE_VIOLATION` — *"La caja #01 ya tiene una sesión abierta"*

## RN-10 · Solo el efectivo afecta al arqueo

**Dónde:** `CajaService`. Se registra movimiento de caja únicamente cuando
`payment_method.affects_cash = true`.
**Cálculo del cierre:**
```
efectivo esperado = monto_apertura + Σ(movimientos de caja)
diferencia        = efectivo contado − efectivo esperado
```
La diferencia (sobrante o faltante) siempre queda registrada, nunca se descarta.

## RN-11 · Una caja cerrada no admite movimientos

Cerrada la sesión, no se aceptan ventas ni movimientos contra ella. Cualquier
corrección posterior exige una autorización explícita y queda auditada.
**Error:** `409 BUSINESS_RULE_VIOLATION` — *"La sesión de caja está cerrada"*

## RN-12 · Las ventas no se eliminan

No existe `DELETE /api/sales/{id}`. Una venta errónea se **anula**, conservando el
registro.

## RN-13 · Anular exige permiso, motivo y autorización

**Flujo:**
```
solicitar anulación → motivo obligatorio → permiso VENTAS_ANULAR →
marcar CANCELLED → devolver stock (movimiento DEVOLUCION) →
revertir efectivo en caja si lo hubo → registrar auditoría
```
Una venta ya `CANCELLED` no se anula dos veces. Si la sesión de caja de la venta
ya está cerrada, la reversión del efectivo se registra contra la sesión abierta
actual, dejando constancia de la sesión original.
**Error sin permiso:** `403 ACCESS_DENIED` + registro de auditoría con `result = DENIED`.

## RN-14 · Una devolución siempre pertenece a una venta

**Validaciones:**
- La venta existe y no está anulada.
- La cantidad devuelta ≤ cantidad vendida − ya devuelta en esa línea.
- Motivo obligatorio.
- `restock` se indica explícitamente por línea.

**Efectos:** si `restock = true` se genera un movimiento `DEVOLUCION` que suma
stock; si el reembolso es en efectivo, se genera un movimiento de caja negativo.
El estado de la venta pasa a `RETURNED` o `PARTIALLY_RETURNED` según se haya
devuelto todo o parte.
**Error:** `409 BUSINESS_RULE_VIOLATION` — *"No se puede devolver 3 unidades: solo se vendieron 2"*

## RN-15 · Los datos oficiales de pago no se editan desde el POS

Titular, número y QR de Yape/Plin requieren `CONFIGURACION_PAGOS`, que ningún
vendedor tiene. El POS los muestra en **solo lectura**, etiquetados como
*"YAPE — CUENTA OFICIAL"*.

## RN-16 · Los descuentos requieren permiso

Aplicar descuento exige `VENTAS_DESCUENTO`. Si además supera el porcentaje
máximo configurado, requiere autorización de un supervisor.
**Error:** `403 ACCESS_DENIED` — *"No tienes permisos para aplicar descuentos"*

## RN-17 · Toda operación sensible se audita

Se registran, como mínimo: login (correcto y fallido), creación y anulación de
ventas, devoluciones, ajustes de inventario, apertura y cierre de caja, cambios
de precio, gestión de usuarios y roles, cambios de configuración, y **todo
intento denegado por falta de permisos**.

La auditoría se escribe con `REQUIRES_NEW`: **persiste aunque la transacción
principal se revierta**, que es precisamente cuando más interesa saber qué se
intentó hacer.

## RN-18 · Contraseñas y bloqueo de cuenta

- BCrypt con factor 12; jamás texto plano ni en logs.
- Mínimo 8 caracteres, con al menos una letra y un número.
- 5 intentos fallidos ⇒ bloqueo de 15 minutos.
- Un login correcto reinicia el contador.
- Un usuario `BLOCKED` o `INACTIVE` no puede autenticarse.

## RN-19 · Los precios y los importes

- `precio > 0`; `precio_promocional`, si existe, debe ser **menor** que el precio.
- Todos los cálculos con `BigDecimal`, escala 2, redondeo `HALF_UP`.
- `subtotal_línea = (precio_unitario × cantidad) − descuento_línea`
- `total_venta = Σ subtotales − descuento_global`
- El descuento nunca puede dejar el total en negativo.

## RN-20 · Autorización por permiso, no por rol

Ningún endpoint comprueba el nombre del rol. Todos usan
`@PreAuthorize("hasAuthority('PERMISO')")`. Así, cambiar qué puede hacer un rol
es modificar datos, no recompilar la aplicación.

---

## Matriz rol → permisos (semilla inicial)

| Permiso | ADMIN | SUPERVISOR | VENDEDOR | ALMACENERO |
|---|:---:|:---:|:---:|:---:|
| `DASHBOARD_VER` | ✔ | ✔ | ✔ | ✔ |
| `PRODUCTOS_CONSULTAR` | ✔ | ✔ | ✔ | ✔ |
| `PRODUCTOS_CREAR` | ✔ | ✔ | | |
| `PRODUCTOS_EDITAR` | ✔ | ✔ | | |
| `PRODUCTOS_ELIMINAR` | ✔ | | | |
| `VARIANTES_GESTIONAR` | ✔ | ✔ | | ✔ |
| `BARCODE_GENERAR` | ✔ | ✔ | | ✔ |
| `INVENTARIO_CONSULTAR` | ✔ | ✔ | ✔ | ✔ |
| `INVENTARIO_ENTRADA` | ✔ | ✔ | | ✔ |
| `INVENTARIO_SALIDA` | ✔ | ✔ | | ✔ |
| `INVENTARIO_AJUSTAR` | ✔ | ✔ | | ✔ |
| `VENTAS_CONSULTAR` | ✔ | ✔ | propias | |
| `VENTAS_CREAR` | ✔ | ✔ | ✔ | |
| `VENTAS_ANULAR` | ✔ | ✔ | | |
| `VENTAS_DESCUENTO` | ✔ | ✔ | | |
| `VENTAS_DEVOLVER` | ✔ | ✔ | | |
| `PROMOTORES_CONSULTAR` | ✔ | ✔ | ✔ | |
| `PROMOTORES_GESTIONAR` | ✔ | ✔ | | |
| `CLIENTES_CONSULTAR` | ✔ | ✔ | ✔ | |
| `CLIENTES_CREAR` | ✔ | ✔ | ✔ | |
| `CLIENTES_EDITAR` | ✔ | ✔ | | |
| `CAJA_ABRIR` | ✔ | ✔ | ✔ | |
| `CAJA_CERRAR` | ✔ | ✔ | | |
| `CAJA_CONSULTAR` | ✔ | ✔ | propia | |
| `CAJA_MOVIMIENTO` | ✔ | ✔ | | |
| `REPORTES_CONSULTAR` | ✔ | ✔ | | |
| `REPORTES_EXPORTAR` | ✔ | ✔ | | |
| `AUDITORIA_CONSULTAR` | ✔ | | | |
| `USUARIOS_CONSULTAR` | ✔ | | | |
| `USUARIOS_CREAR` | ✔ | | | |
| `USUARIOS_EDITAR` | ✔ | | | |
| `USUARIOS_BLOQUEAR` | ✔ | | | |
| `ROLES_GESTIONAR` | ✔ | | | |
| `CONFIGURACION_VER` | ✔ | ✔ | | |
| `CONFIGURACION_EDITAR` | ✔ | | | |
| `CONFIGURACION_PAGOS` | ✔ | | | |

*"propias" / "propia"*: el permiso se concede, pero el service filtra los
resultados al usuario autenticado.
