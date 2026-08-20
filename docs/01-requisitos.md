# 01 — Requisitos

**Producto:** Sistema Integral de Gestión — Freestyle Perú
**Fase actual:** Fase 0 (análisis y diseño) → Fase 2 (backend)
**Fecha:** 19/08/2026

---

## 1. Objetivo

Software de gestión para una tienda de ropa física, con operación diaria real:
catálogo, variantes, códigos de barras, inventario trazable, punto de venta,
pagos, caja, clientes, devoluciones, reportes y auditoría.

La Fase 1 cubre **la operación interna de una tienda**. La arquitectura no debe
bloquear las fases posteriores (ecommerce, IA, WhatsApp, multisucursal).

## 2. Alcance de la Fase 1

### Dentro de alcance

| Módulo | Contenido |
|---|---|
| Autenticación | Login, logout, cambio de contraseña, bloqueo por intentos, expiración de sesión |
| Usuarios | CRUD de usuarios, asignación de roles, bloqueo/desbloqueo |
| Roles y permisos | Roles del sistema + permisos granulares por acción |
| Catálogo | Categorías, subcategorías, marcas, colores, tallas |
| Productos | CRUD, búsqueda, filtros, imagen, precio y precio promocional |
| Variantes | Combinación color+talla, SKU propio, código de barras, stock |
| Códigos de barras | Generación EAN-13, validación de duplicados, búsqueda por escaneo |
| Inventario | Stock, stock mínimo, movimientos inmutables, ajustes, entradas, salidas |
| POS | Escaneo, búsqueda, carrito, descuentos, cliente, cobro, ticket |
| Ventas | Registro transaccional, consulta, detalle |
| Pagos | Efectivo, Yape, Plin, tarjeta, transferencia y **pago mixto** |
| Caja | Apertura, movimientos, cierre con arqueo y diferencia |
| Clientes | CRUD, historial de compras, totales acumulados |
| Devoluciones | Asociadas a una venta, con o sin retorno a stock |
| Anulaciones | Con motivo, autorización y reversión de efectos |
| Auditoría | Registro de operaciones sensibles |
| Reportes | Ventas, inventario, caja y auditoría con filtros de fecha |
| Configuración | Empresa, moneda, catálogos, datos de pago, usuarios |

### Fuera de alcance en Fase 1

- Ecommerce y catálogo público.
- IA generativa y recomendaciones.
- Integración con WhatsApp.
- Multisucursal operativo (la arquitectura lo permite, no se implementa la UI).
- Venta al crédito / fiado (requiere autorización expresa).
- Facturación electrónica SUNAT (nota comercial interna, no comprobante fiscal).
- Proveedores y precio de compra.

## 3. Actores

| Actor | Descripción |
|---|---|
| Administrador | Control total, incluida configuración y autorizaciones |
| Supervisor | Ventas, caja, inventario, reportes, clientes; autoriza anulaciones |
| Vendedor | Vende, consulta productos, registra clientes, ve sus propias ventas |
| Almacenero | Consulta productos, registra entradas/salidas, ajusta inventario |

## 4. Requisitos funcionales

### RF-AUTH
- **RF-AUTH-01** El usuario inicia sesión con usuario y contraseña.
- **RF-AUTH-02** Las contraseñas se almacenan con hash BCrypt, nunca en claro.
- **RF-AUTH-03** Tras 5 intentos fallidos la cuenta se bloquea 15 minutos.
- **RF-AUTH-04** La sesión expira; se renueva mediante refresh token.
- **RF-AUTH-05** El usuario puede cambiar su contraseña; se puede forzar el cambio en el primer acceso.

### RF-PROD
- **RF-PROD-01** Un producto es el modelo general; las variantes son las combinaciones concretas.
- **RF-PROD-02** Una variante es única por producto + color + talla.
- **RF-PROD-03** El SKU y el código de barras son campos distintos e independientes.
- **RF-PROD-04** El código de barras es único en todo el sistema.
- **RF-PROD-05** El sistema genera códigos EAN-13 válidos con dígito verificador.
- **RF-PROD-06** Se puede buscar una variante por código de barras en una sola operación.

### RF-INV
- **RF-INV-01** El stock nunca se modifica sin generar un movimiento de inventario.
- **RF-INV-02** Los movimientos son inmutables: no se editan ni se borran.
- **RF-INV-03** Cada movimiento guarda stock anterior, cantidad, stock nuevo, usuario y fecha.
- **RF-INV-04** No se puede vender una variante sin stock suficiente.
- **RF-INV-05** Existe historial completo consultable por variante.

### RF-POS
- **RF-POS-01** Se agrega producto al carrito por escaneo, SKU, nombre o categoría.
- **RF-POS-02** Se modifica cantidad, se elimina línea y se vacía el carrito.
- **RF-POS-03** El descuento requiere permiso explícito.
- **RF-POS-04** Se puede asociar un cliente a la venta (opcional).
- **RF-POS-05** La venta admite varios pagos (pago mixto).
- **RF-POS-06** No se cierra la venta si la suma de pagos ≠ total.
- **RF-POS-07** La venta requiere una sesión de caja abierta.

### RF-CAJA
- **RF-CAJA-01** Cada caja tiene sesiones con apertura y cierre.
- **RF-CAJA-02** Solo puede existir una sesión abierta por caja simultáneamente.
- **RF-CAJA-03** Al cerrar se calcula efectivo esperado, efectivo real y diferencia.
- **RF-CAJA-04** Solo los pagos en efectivo afectan el arqueo.
- **RF-CAJA-05** Una caja cerrada no admite movimientos nuevos.

### RF-VENTA
- **RF-VENTA-01** Las ventas no se eliminan físicamente jamás.
- **RF-VENTA-02** La anulación exige motivo, permiso y queda auditada.
- **RF-VENTA-03** La anulación revierte inventario y efectos de caja.
- **RF-VENTA-04** Una devolución siempre se asocia a una venta existente.
- **RF-VENTA-05** La devolución decide explícitamente si el producto regresa al stock.
- **RF-VENTA-06** No se devuelve más cantidad de la vendida.

### RF-AUD
- **RF-AUD-01** Se registran usuario, acción, entidad, registro, valor anterior, valor nuevo, resultado, IP y fecha.
- **RF-AUD-02** Se registran también los intentos denegados por falta de permisos.
- **RF-AUD-03** Los registros de auditoría son de solo lectura.

## 5. Requisitos no funcionales

| Código | Requisito |
|---|---|
| RNF-01 | El POS responde a una búsqueda por código de barras en < 200 ms |
| RNF-02 | Toda operación financiera es transaccional (todo o nada) |
| RNF-03 | Interfaz responsive: escritorio, laptop, tablet y móvil |
| RNF-04 | Sin credenciales ni secretos en el código fuente |
| RNF-05 | Errores con formato uniforme, sin stack traces al cliente |
| RNF-06 | Los importes usan `DECIMAL`, nunca coma flotante |
| RNF-07 | Contraste de texto conforme a WCAG AA |
| RNF-08 | El backend valida siempre, aunque el frontend ya haya validado |
| RNF-09 | Migraciones de base de datos versionadas y reproducibles |
| RNF-10 | Zona horaria fija `America/Lima` en toda la aplicación |

## 6. Reglas de negocio críticas

Detalladas en [04-reglas-negocio.md](04-reglas-negocio.md). Resumen:

1. No vender sin stock suficiente.
2. No duplicar códigos de barras.
3. No duplicar SKU.
4. No eliminar ventas físicamente.
5. Las anulaciones requieren permiso.
6. Los cambios sensibles generan auditoría.
7. Cerrar caja requiere permiso.
8. Los datos oficiales de pago no se editan desde el POS.
9. Los movimientos de inventario son inmutables.
10. Las operaciones financieras son trazables.

## 7. Criterios de aceptación de la Fase 1

El recorrido completo debe ser posible de principio a fin:

```
LOGIN → DASHBOARD → PRODUCTOS → CREAR PRODUCTO → CREAR VARIANTES →
ASIGNAR SKU → ASIGNAR CÓDIGO DE BARRAS → VER INVENTARIO → ABRIR POS →
ESCANEAR PRODUCTO → CARRITO → CLIENTE → PAGO → CONFIRMAR VENTA →
VER VENTA → VER MOVIMIENTO DE INVENTARIO → VER MOVIMIENTO DE CAJA →
VER AUDITORÍA → VER REPORTE
```

## 8. Supuestos y decisiones tomadas

| # | Supuesto | Justificación |
|---|---|---|
| S-01 | Moneda única: Soles (PEN) | Tienda peruana; el código de moneda es configurable |
| S-02 | Los precios se guardan con IGV incluido | Práctica habitual del retail peruano en tienda física |
| S-03 | El sistema emite nota interna, no comprobante SUNAT | La facturación electrónica es un proyecto aparte |
| S-04 | Una sucursal y un almacén en Fase 1 | El modelo ya contempla varios; la UI no lo expone |
| S-05 | SKU de variante derivado del SKU de producto + color + talla | Legible para el personal de tienda |
| S-06 | Códigos de barras EAN-13 con prefijo interno `775` | Rango de uso interno, no registrado en GS1 |

## 9. Preguntas abiertas

- ¿Se necesita impresión de ticket en impresora térmica (ESC/POS) o basta con PDF/navegador?
- ¿El descuento se aplica por línea, por total, o ambos? *(Diseñado: ambos)*
- ¿Existe más de una caja física en la tienda? *(Diseñado: soporta varias)*
