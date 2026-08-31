# ================================================================
# PROMPT MAESTRO — INTEGRACIÓN NIUBIZ + VERIFACT + SUNAT
## SaaS Multi-Tenant de Ventas, POS, Inventario y Comercio
# ================================================================

Actúa como un Arquitecto de Software Senior y Desarrollador
Full-Stack Senior especializado en:

- SaaS Multi-Tenant
- Sistemas POS
- Sistemas de ventas
- Retail
- E-commerce
- Pasarelas de pago
- Niubiz
- Facturación electrónica
- VERIFACT
- SUNAT
- APIs REST
- Integraciones de terceros
- Seguridad
- Arquitecturas escalables
- Sistemas transaccionales
- Bases de datos
- Backend
- Frontend

## 0. Contexto Fundamental

Estoy trabajando sobre un SISTEMA DE VENTAS YA EXISTENTE.

Este sistema NO debe ser reconstruido desde cero.

Actualmente ya cuenta con módulos funcionales como:

- Dashboard
- Productos
- Inventario
- Ventas / POS
- Caja
- Clientes
- Pedidos
- Separaciones
- Combos
- Promociones
- Reportes
- Auditoría
- Usuarios
- Configuración

El sistema está orientado a empresas que venden productos físicos,
por ejemplo:

- Tiendas de ropa
- Calzado
- Accesorios
- Electrónica
- Cosméticos
- Tiendas de variedades
- Retail
- Comercios generales
- Cualquier negocio que comercialice productos

## 1. El Sistema Es Un Saas Multi-Tenant

ESTA ES UNA DE LAS REGLAS MÁS IMPORTANTES DEL PROYECTO.

El sistema es un SaaS.

Una única plataforma es utilizada por múltiples negocios.

Cada negocio representa un TENANT independiente.

Ejemplo:

QYNEX SAAS
│
├── Tenant A
│   └── Tienda ABC
│
├── Tenant B
│   └── Tienda XYZ
│
├── Tenant C
│   └── Boutique DEF
│
└── Tenant D
    └── Tienda GHI

Cada tenant tiene sus propios:

- usuarios
- roles
- productos
- categorías
- inventario
- clientes
- ventas
- pedidos
- separaciones
- combos
- promociones
- cajas
- movimientos de caja
- pagos
- comprobantes
- configuraciones
- reportes
- auditoría
- configuraciones Niubiz
- configuraciones VERIFACT
- información tributaria

## 2. Regla Absoluta De Aislamiento

NUNCA se deben mezclar datos entre tenants.

Tenant A NO puede acceder a:

- productos de Tenant B
- inventario de Tenant B
- clientes de Tenant B
- ventas de Tenant B
- pedidos de Tenant B
- caja de Tenant B
- pagos de Tenant B
- comprobantes de Tenant B
- configuración Niubiz de Tenant B
- configuración VERIFACT de Tenant B
- RUC de Tenant B
- credenciales de Tenant B

El aislamiento debe aplicarse en BACKEND.

NO confiar únicamente en filtros del frontend.

NO permitir que el frontend determine arbitrariamente el tenant.

Utilizar el mecanismo de tenancy existente en el proyecto.

NO crear otro sistema de multi-tenancy si ya existe uno.

## 3. No Reescribir El Sistema

NO quiero que construyas un sistema nuevo.

NO quiero que reemplaces funcionalidades existentes.

NO quiero que rehagas:

- POS
- Ventas
- Productos
- Inventario
- Caja
- Clientes
- Pedidos
- Separaciones
- Combos
- Promociones
- Reportes
- Auditoría
- Usuarios
- Configuración

Tu objetivo es INTEGRAR nuevas capacidades dentro del sistema existente.

Si una entidad, servicio, componente o módulo ya existe:

UTILÍZALO.

Si necesita ampliarse:

EXTIÉNDELO.

No dupliques funcionalidades.

## 4. Objetivo Principal

Integrar profesionalmente:

## 1. Niubiz
## 2. Verifact
## 3. Sunat

dentro del SaaS existente.

La arquitectura debe permitir que cada negocio utilice sus propias
credenciales/configuración.

Conceptualmente:

TENANT
   │
   ├── Ventas
   ├── Inventario
   ├── Caja
   ├── Clientes
   │
   ├── Niubiz Configuration
   │
   └── VERIFACT Configuration

El SaaS funciona como ORQUESTADOR TECNOLÓGICO.

El SaaS NO debe convertirse en el comercio que realiza las ventas
de todos los negocios.

## 5. Concepto De Niubiz

Cada negocio/tenant debe ser considerado como un comercio independiente
respecto a Niubiz.

Ejemplo:

Tenant A
RUC: RUC_A
Merchant ID: MERCHANT_A

Tenant B
RUC: RUC_B
Merchant ID: MERCHANT_B

Cuando Tenant A realiza una venta:

Tenant A
 ↓
Venta A
 ↓
Payment A
 ↓
Configuración Niubiz A
 ↓
Merchant A
 ↓
Niubiz
 ↓
Resultado del pago

Cuando Tenant B realiza una venta:

Tenant B
 ↓
Venta B
 ↓
Payment B
 ↓
Configuración Niubiz B
 ↓
Merchant B
 ↓
Niubiz
 ↓
Resultado del pago

NUNCA:

Tenant A
 ↓
Merchant B

## 6. Arquitectura Niubiz

NO acoplar directamente:

SaleService
   ↓
Niubiz API

Preferir una arquitectura desacoplada:

SaleService
   ↓
PaymentService
   ↓
PaymentProvider
   ↓
NiubizProvider
   ↓
Niubiz API

Conceptualmente:

PaymentService
      │
      ▼
PaymentProvider
      │
      ├── NiubizProvider
      │
      └── Futuro proveedor

Esto debe permitir agregar otros proveedores de pago
sin modificar el Core de ventas.

## 7. Checkout Niubiz

Cuando el cliente seleccione Niubiz:

POS / CHECKOUT
       ↓
Crear venta
       ↓
Crear intento de pago
       ↓
Obtener Tenant
       ↓
Obtener configuración Niubiz del Tenant
       ↓
Obtener credenciales correspondientes
       ↓
Crear/iniciar transacción Niubiz
       ↓
Cliente completa el pago
       ↓
Niubiz responde
       ↓
Backend valida resultado
       ↓
Actualizar Payment
       ↓
Actualizar Venta
       ↓
Continuar flujo

NO confiar únicamente en el frontend.

El backend debe validar el resultado real de la operación.

## 8. Checkout Api / Formulario Desacoplado

Utilizar el mecanismo de integración que Niubiz soporte oficialmente
para el flujo requerido.

La implementación debe priorizar un checkout desacoplado/API cuando
la documentación oficial vigente lo permita.

Objetivos:

- mantener el flujo dentro de la experiencia del negocio
- evitar acoplamiento innecesario
- mantener el branding del comercio
- permitir integrar el pago dentro del flujo de venta
- permitir escalar a múltiples comercios

Ejemplo:

Cliente
 ↓
Selecciona productos
 ↓
Checkout del negocio
 ↓
Selecciona Niubiz
 ↓
Pago
 ↓
Confirmación
 ↓
Comprobante

NO asumir que el SaaS es el comercio que cobra.

El comercio correspondiente es el tenant.

## 9. Documentación Oficial Niubiz

ANTES DE IMPLEMENTAR LA INTEGRACIÓN REAL:

Consulta la documentación oficial y vigente de Niubiz.

Debes verificar:

- autenticación
- merchantId
- credenciales
- endpoints
- ambientes
- sandbox
- producción
- checkout
- sesión
- transacción
- respuesta
- códigos
- estados
- callbacks
- webhooks
- reversos
- anulaciones
- reembolsos
- seguridad

NO inventar:

- endpoints
- URLs
- parámetros
- headers
- respuestas
- códigos
- credenciales
- mecanismos de autenticación

La documentación oficial vigente es la fuente de verdad.

## 10. Estados De Pago

Implementar/adaptar estados claros.

Conceptualmente:

CREATED
PENDING
PROCESSING
APPROVED
DECLINED
FAILED
CANCELLED
REFUNDED
ERROR

Los estados finales deben adaptarse a los estados/códigos reales
de Niubiz.

## 11. Idempotencia De Pagos

OBLIGATORIO.

Evitar cobros duplicados causados por:

- doble clic
- refresh
- retry
- timeout
- error de red
- callback duplicado
- webhook duplicado
- reintentos automáticos

Cada intento de pago debe tener una referencia única.

Si Niubiz proporciona mecanismos oficiales de idempotencia,
utilizarlos.

## 12. Relación Venta / Payment

Si el sistema ya posee una entidad Payment:

UTILIZARLA.

Si no existe y la arquitectura lo requiere, crear una entidad
independiente de pago.

Conceptualmente:

Sale
   │
   └── Payment

Ejemplo:

SALE #000123

Subtotal:
S/ 100

Descuento:
S/ 10

Total:
S/ 90

Payment:

Provider:
NIUBIZ

Amount:
S/ 90

Status:
APPROVED

No mezclar innecesariamente toda la información de Niubiz
dentro de Sale.

## 13. Pago Y Confirmación De Venta

Para pagos Niubiz:

Venta
 ↓
PENDING_PAYMENT
 ↓
Niubiz
 ↓
APPROVED
 ↓
CONFIRMED

Si:

DECLINED
FAILED
CANCELLED

NO marcar la venta como pagada.

Adaptar los estados a la arquitectura existente.

## 14. Caja

Cuando Niubiz confirme correctamente el pago:

Payment
 ↓
APPROVED
 ↓
Registrar movimiento de caja
 ↓
Actualizar caja

Ejemplo:

Venta:
S/ 150

Método:
Niubiz

Payment:
APPROVED

Caja:
+ S/ 150

NO registrar el ingreso antes de confirmar correctamente el pago.

NO duplicar movimientos de caja ante callbacks/retries.

## 15. Inventario

NO modificar arbitrariamente la lógica existente de inventario.

Primero determinar cuándo el sistema actual descuenta stock.

Puede ser:

- creación de venta
- confirmación
- pago
- despacho
- entrega

Mantener la lógica actual.

Si actualmente:

Pago aprobado
 ↓
Venta confirmada
 ↓
Descuento de inventario

mantener ese comportamiento.

Evitar doble descuento.

## 16. Pedidos

Si ya existe módulo de Pedidos:

utilizarlo.

Conceptualmente:

Pedido
 ↓
Checkout
 ↓
Payment
 ↓
Niubiz
 ↓
APPROVED
 ↓
Pedido confirmado
 ↓
Venta

NO crear un Order Engine paralelo.

## 17. Separaciones

Si el sistema actual permite separaciones:

Integrar Niubiz con el flujo existente.

Ejemplo:

Producto:
S/ 500

Separación:
S/ 100

Pago:
Niubiz

 ↓

Payment APPROVED

 ↓

Registrar abono

 ↓

Saldo:

S/ 400

No romper la lógica existente.

## 18. Pagos Parciales

Si el sistema ya soporta pagos parciales:

debe ser compatible con Niubiz.

Ejemplo:

Venta:
S/ 1,000

Pago 1:
S/ 300
Niubiz

Pago 2:
S/ 700
Efectivo

Total:
S/ 1,000

NO implementar una funcionalidad paralela si el sistema actual
no la soporta.

## 19. Reembolsos

Si Niubiz/API utilizada permite reembolsos:

preparar soporte para:

Payment
   ↓
Refund

Nunca eliminar la transacción original.

Mantener trazabilidad.

## 20. Configuración Niubiz Por Tenant

Dentro del módulo:

Configuración
   ↓
Pagos
   ↓
Niubiz

Cada tenant debe poder tener su propia configuración.

Conceptualmente:

Tenant A
 └── Niubiz Configuration A

Tenant B
 └── Niubiz Configuration B

La interfaz debe mostrar solamente la configuración correspondiente
al tenant actual.

Los campos exactos deben determinarse según la documentación oficial
de Niubiz.

Puede incluir conceptualmente:

- Merchant ID
- credenciales
- ambiente
- estado
- configuración requerida

NO inventar campos que Niubiz no utilice.

## 21. Seguridad Niubiz

Las credenciales privadas:

NUNCA deben aparecer en:

- frontend
- JavaScript público
- localStorage
- sessionStorage
- HTML
- logs
- respuestas API
- auditoría visible

Deben permanecer en backend/secret management.

Si se almacenan en base de datos:

protegerlas/cifrarlas adecuadamente.

## 22. Multi-Tenant + Niubiz

El flujo obligatorio es:

REQUEST
 ↓
AUTHENTICATION
 ↓
USER
 ↓
TENANT
 ↓
AUTHORIZATION
 ↓
SALE
 ↓
PAYMENT
 ↓
TENANT PAYMENT CONFIGURATION
 ↓
NIUBIZ MERCHANT DEL TENANT
 ↓
NIUBIZ

El PaymentService NUNCA debe aceptar ciegamente un
merchantId enviado desde frontend.

El backend debe resolver el merchant correspondiente
al tenant autenticado.

## 23. Facturación Electrónica

Ahora integrar VERIFACT + SUNAT.

NO acoplar:

SaleService
   ↓
Verifact API

Preferir:

SaleService
   ↓
BillingService
   ↓
ElectronicInvoicingProvider
   ↓
VerifactProvider
   ↓
VERIFACT
   ↓
SUNAT

## 24. Verifact

La integración con VERIFACT debe utilizar la documentación oficial
vigente.

ANTES DE PROGRAMAR:

verificar:

- API
- autenticación
- endpoints
- ambientes
- sandbox
- producción
- requests
- responses
- comprobantes
- XML
- PDF
- CDR
- estados
- errores
- anulaciones
- notas de crédito
- notas de débito
- reintentos

NO inventar APIs.

NO inventar endpoints.

NO inventar URLs.

NO inventar parámetros.

NO inventar headers.

NO inventar mecanismos de autenticación.

La documentación oficial vigente de VERIFACT es la fuente de verdad.

## 25. Verifact Por Tenant

Cada negocio debe utilizar su propia configuración tributaria.

Ejemplo:

Tenant A
 ↓
RUC A
 ↓
Configuración VERIFACT A
 ↓
Comprobante A
 ↓
SUNAT

Tenant B
 ↓
RUC B
 ↓
Configuración VERIFACT B
 ↓
Comprobante B
 ↓
SUNAT

NUNCA:

Tenant A
 ↓
RUC B

ni:

Tenant A
 ↓
Credenciales VERIFACT B

## 26. Configuración De Facturación

Dentro de:

Configuración
   ↓
Facturación Electrónica

permitir configurar, según corresponda:

- datos fiscales
- RUC
- razón social
- dirección
- establecimiento
- series
- ambiente
- credenciales
- configuración VERIFACT
- estado de integración

Los campos exactos deben adaptarse a VERIFACT y SUNAT.

## 27. Sunat

La integración debe respetar la normativa vigente de SUNAT.

El sistema debe distinguir claramente:

PAYMENT STATUS

de:

BILLING STATUS

Un pago aprobado NO significa automáticamente que el comprobante
haya sido aceptado por SUNAT.

Ejemplo:

Payment:
APPROVED

Billing:
REJECTED

Esto debe ser posible y manejable.

## 28. Tipos De Comprobantes

Preparar soporte para:

- Boleta electrónica
- Factura electrónica
- Nota de crédito
- Nota de débito

Siempre respetando:

- reglas SUNAT
- documentación VERIFACT
- reglas correspondientes al tipo de comprobante

## 29. Checkout Y Tipo De Comprobante

En el POS/Checkout:

Tipo de comprobante:

○ Boleta
○ Factura

Si selecciona FACTURA:

solicitar los datos requeridos.

Conceptualmente:

- RUC
- razón social
- dirección fiscal

Si selecciona BOLETA:

solicitar los datos requeridos según la normativa vigente.

Validar antes de enviar a VERIFACT.

## 30. Clientes

UTILIZAR la entidad Cliente existente.

NO crear innecesariamente:

- WebCustomer
- BillingCustomer
- VerifactCustomer

si ya existe Customer/Cliente.

Utilizar los datos existentes.

Debe contemplarse según corresponda:

- DNI
- RUC
- nombre
- razón social
- dirección
- email

## 31. Relación Sale / Electronic Document

Una venta puede generar un comprobante.

Conceptualmente:

Sale
   │
   └── ElectronicDocument

Ejemplo:

SALE #1001

Total:
S/ 250

Tipo:
BOLETA

 ↓

BillingService

 ↓

VERIFACT

 ↓

SUNAT

 ↓

ACCEPTED

Guardar la relación entre venta y comprobante.

## 32. Series Y Correlativos

Primero auditar si ya existe:

- series
- correlativos
- establecimientos
- tipos de comprobante

Si existe:

REUTILIZAR.

La generación debe ser:

- segura
- concurrente
- transaccional
- multi-tenant
- auditable

Nunca generar duplicados.

Ejemplo incorrecto:

B001-000001
B001-000001

para dos documentos diferentes.

## 33. Estados De Facturación

Conceptualmente:

DRAFT
GENERATED
PENDING
SENT
ACCEPTED
REJECTED
CANCELLED
ERROR

Los estados reales deben adaptarse a VERIFACT/SUNAT.

## 34. Reintentos De Facturación

Si VERIFACT o SUNAT presenta un error temporal:

NO crear otra venta.

NO crear otro comprobante innecesariamente.

Permitir retry.

Ejemplo:

Sale #1001
Payment:
APPROVED

Invoice:
PENDING

 ↓

Retry Billing

Debe continuar utilizando la misma venta y el flujo correcto
de emisión.

## 35. Idempotencia De Facturación

Evitar:

Venta #1001
 ↓
Factura 1
Factura 2
Factura 3

por:

- doble clic
- timeout
- retry
- error de red
- callback duplicado

Utilizar identificadores únicos y mecanismos de idempotencia.

## 36. Separar Payment Status Y Billing Status

Esta separación es OBLIGATORIA.

Ejemplo:

SALE
 │
 ├── PAYMENT
 │      └── APPROVED
 │
 └── BILLING
        └── REJECTED

Otro ejemplo:

SALE
 │
 ├── PAYMENT
 │      └── APPROVED
 │
 └── BILLING
        └── PENDING

Otro:

SALE
 │
 ├── PAYMENT
 │      └── DECLINED
 │
 └── BILLING
        └── NOT_ISSUED

No mezclar ambos estados.

## 37. Flujo Completo Del Saas

El flujo final debe ser:

CLIENTE
 ↓
POS / CHECKOUT
 ↓
TENANT
 ↓
CREAR VENTA
 ↓
SELECCIONAR COMPROBANTE
 ↓
SELECCIONAR NIUBIZ
 ↓
CREAR PAYMENT
 ↓
NIUBIZ CONFIGURATION DEL TENANT
 ↓
NIUBIZ
 ↓
PAGO APROBADO
 ↓
CONFIRMAR PAYMENT
 ↓
CONFIRMAR VENTA
 ↓
CAJA
 ↓
INVENTARIO
 ↓
BILLING SERVICE
 ↓
VERIFACT CONFIGURATION DEL TENANT
 ↓
VERIFACT
 ↓
SUNAT
 ↓
COMPROBANTE ACEPTADO
 ↓
GUARDAR COMPROBANTE
 ↓
DISPONIBILIZAR COMPROBANTE
 ↓
CLIENTE

## 38. Ejemplo Multi-Tenant Real

TENANT A:

Tienda:
Moda ABC

RUC:
20111111111

Merchant Niubiz:
MERCHANT_A

Configuración VERIFACT:
CONFIG_A

Realiza:

Venta:
S/ 300

 ↓

Niubiz A

 ↓

Payment APPROVED

 ↓

Factura A

 ↓

VERIFACT A

 ↓

SUNAT

--------------------------------------------------

TENANT B:

Tienda:
Fashion XYZ

RUC:
20222222222

Merchant Niubiz:
MERCHANT_B

Configuración VERIFACT:
CONFIG_B

Realiza:

Venta:
S/ 500

 ↓

Niubiz B

 ↓

Payment APPROVED

 ↓

Factura B

 ↓

VERIFACT B

 ↓

SUNAT

Las operaciones son completamente independientes.

## 39. Administración Del Saas

Debe existir una separación conceptual entre:

## 1. Super Admin Del Saas
## 2. Administrador Del Tenant
## 3. Usuarios Del Tenant

El Super Admin administra la plataforma según sus permisos.

El administrador de Tenant administra únicamente su negocio.

Un usuario de Tenant A jamás debe poder operar sobre Tenant B.

## 40. Credenciales Por Tenant

NO utilizar una única variable global para todas las empresas.

INCORRECTO:

NIUBIZ_MERCHANT_ID=XXXX

si eso pretende representar todos los tenants.

CORRECTO CONCEPTUALMENTE:

Tenant A
 └── NiubizConfig A

Tenant B
 └── NiubizConfig B

Tenant C
 └── NiubizConfig C

Lo mismo para VERIFACT.

Las variables de entorno pueden contener secretos globales
de infraestructura, pero NO deben utilizarse como almacenamiento
de las credenciales individuales de cada tenant.

## 41. Encriptación

Las credenciales sensibles almacenadas deben protegerse.

Nunca almacenar secretos sensibles en texto plano si la arquitectura
permite cifrado.

Nunca devolver secretos completos al frontend.

Nunca incluir secretos en:

- logs
- errores
- respuestas
- auditoría
- reportes

## 42. Reportes

Utilizar el módulo Reportes existente.

Agregar información de:

VENTAS

- ventas totales
- ventas pagadas
- ventas pendientes

PAGOS

- pagos Niubiz
- aprobados
- rechazados
- pendientes
- reembolsados

FACTURACIÓN

- facturas
- boletas
- notas de crédito
- notas de débito
- aceptados
- rechazados
- pendientes

Todos los reportes deben estar filtrados por tenant.

Un Tenant A nunca debe ver reportes de Tenant B.

## 43. Auditoría

Utilizar el módulo Auditoría existente.

Registrar:

PaymentCreated
PaymentProcessing
PaymentApproved
PaymentDeclined
PaymentFailed
PaymentRefunded

InvoiceCreated
InvoiceSubmitted
InvoiceAccepted
InvoiceRejected
InvoiceCancelled
BillingRetry
BillingError

Registrar:

- tenant
- usuario
- fecha
- operación
- referencia
- resultado

NO registrar secretos sensibles.

## 44. Seguridad

Aplicar:

- autenticación
- autorización
- RBAC
- tenant isolation
- validación server-side
- sanitización
- rate limiting donde corresponda
- protección de credenciales
- auditoría
- idempotencia

NUNCA confiar en valores enviados por frontend para:

- precio
- total
- tenant
- merchantId
- estado del pago
- RUC
- configuración tributaria

El backend debe ser la fuente de verdad.

## 45. Consistencia Transaccional

Prestar especial atención a:

PAYMENT
VENTA
CAJA
INVENTARIO
FACTURACIÓN

Evitar:

Pago aprobado
pero venta no confirmada

Pago duplicado

Caja duplicada

Inventario descontado dos veces

Comprobante duplicado

Venta duplicada

Cruce entre tenants

Cuando corresponda utilizar:

- database transactions
- locks
- unique constraints
- idempotency keys
- retries controlados
- compensating actions

## 46. Manejo De Errores

Manejar correctamente:

NIUBIZ:

- API no disponible
- timeout
- credenciales incorrectas
- configuración incompleta
- pago rechazado
- transacción fallida
- callback duplicado

VERIFACT:

- API no disponible
- timeout
- autenticación incorrecta
- configuración incompleta
- comprobante rechazado
- datos inválidos

SUNAT:

- indisponibilidad
- rechazo
- errores de validación

Los mensajes para el usuario deben ser comprensibles.

Los detalles técnicos deben ir a logs seguros.

## 47. Configuración

Dentro del módulo existente:

CONFIGURACIÓN

organizar:

Configuración
│
├── Negocio
├── Impuestos
├── Comprobantes
├── Series
├── Métodos de pago
│
├── Niubiz
│
└── Facturación electrónica
     └── VERIFACT

Todo debe respetar el tenant actual.

## 48. Arquitectura Final

La arquitectura conceptual debe aproximarse a:

                         QYNEX SAAS
                              │
                       TENANT MANAGER
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
       TENANT A            TENANT B            TENANT C
          │                   │                   │
       POS/VENTAS          POS/VENTAS          POS/VENTAS
          │                   │                   │
          ▼                   ▼                   ▼
   PAYMENT SERVICE      PAYMENT SERVICE      PAYMENT SERVICE
          │                   │                   │
          ▼                   ▼                   ▼
   NiubizProvider A      NiubizProvider B      NiubizProvider C
          │                   │                   │
          ▼                   ▼                   ▼
      NIUBIZ A             NIUBIZ B             NIUBIZ C
          │                   │                   │
          └───────────────────┼───────────────────┘
                              │
                           VENTA
                              │
                     ┌────────┴────────┐
                     │                 │
                    CAJA           INVENTARIO
                     │                 │
                     └────────┬────────┘
                              │
                       BILLING SERVICE
                              │
                  ElectronicInvoicingProvider
                              │
                     VerifactProvider
                              │
                    ┌─────────┴─────────┐
                    │                   │
                 VERIFACT A          VERIFACT B
                    │                   │
                    ▼                   ▼
                 SUNAT A             SUNAT B

## 49. Principio De Desacoplamiento

NO quiero:

SaleService
 ↓
Niubiz

ni:

SaleService
 ↓
VERIFACT

Preferir:

SaleService
 ↓
PaymentService
 ↓
PaymentProvider
 ↓
NiubizProvider

y:

SaleService
 ↓
BillingService
 ↓
ElectronicInvoicingProvider
 ↓
VerifactProvider

Esto permite cambiar proveedores posteriormente.

Por ejemplo:

PaymentProvider:

- Niubiz
- Otro proveedor futuro

ElectronicInvoicingProvider:

- VERIFACT
- Otro proveedor futuro

## 50. No Duplicar Entidades

Si ya existen:

Sale
Customer
Product
Order
Payment
Invoice
CashMovement
InventoryMovement

UTILIZARLAS.

NO crear innecesariamente:

NewSale
NewCustomer
NewProduct
NewOrder
NewInvoice

solamente para las integraciones.

Extender las existentes.

## 51. Testing Multi-Tenant

Crear pruebas específicamente para demostrar aislamiento.

TEST:

Tenant A
Merchant A

Tenant B
Merchant B

Venta A:

Debe utilizar Merchant A.

Venta B:

Debe utilizar Merchant B.

Factura A:

Debe utilizar RUC/configuración A.

Factura B:

Debe utilizar RUC/configuración B.

Intentar desde Tenant A acceder a una venta de Tenant B:

DEBE FALLAR.

Intentar utilizar Merchant B desde Tenant A:

DEBE FALLAR.

Intentar utilizar configuración VERIFACT B desde Tenant A:

DEBE FALLAR.

## 52. Testing Niubiz

Probar:

- pago aprobado
- pago rechazado
- timeout
- error
- retry
- callback duplicado
- webhook duplicado
- monto incorrecto
- transacción inexistente
- credenciales inválidas
- tenant incorrecto

## 53. Testing Verifact / Sunat

Probar:

- boleta aceptada
- factura aceptada
- comprobante rechazado
- timeout
- retry
- RUC inválido
- datos incompletos
- comprobante duplicado
- error de autenticación
- error temporal
- nota de crédito
- nota de débito

## 54. Test End-To-End

Crear al menos un flujo completo:

TENANT
 ↓
PRODUCTO
 ↓
POS
 ↓
CLIENTE
 ↓
VENTA
 ↓
BOLETA/FACTURA
 ↓
NIUBIZ
 ↓
PAGO APROBADO
 ↓
VENTA CONFIRMADA
 ↓
CAJA
 ↓
INVENTARIO
 ↓
VERIFACT
 ↓
SUNAT
 ↓
COMPROBANTE ACEPTADO
 ↓
CLIENTE

## 55. Escalabilidad

La solución debe funcionar para:

10 tenants
100 tenants
1,000 tenants
10,000 tenants

sin modificar la lógica central.

No crear código específico para un número determinado de negocios.

Los servicios deben ser reutilizables.

Ejemplo:

PaymentService
 ↓
Tenant Configuration
 ↓
PaymentProvider
 ↓
NiubizProvider

BillingService
 ↓
Tenant Configuration
 ↓
ElectronicInvoicingProvider
 ↓
VerifactProvider

## 56. Plan De Implementación

NO empieces programando inmediatamente.

PRIMERO AUDITA EL PROYECTO.

FASE 1
Auditar arquitectura.

FASE 2
Identificar arquitectura multi-tenant.

FASE 3
Identificar cómo se determina el tenant.

FASE 4
Identificar entidades existentes.

FASE 5
Identificar flujo actual de ventas.

FASE 6
Identificar flujo actual de caja.

FASE 7
Identificar flujo actual de inventario.

FASE 8
Identificar flujo actual de clientes.

FASE 9
Identificar flujo actual de pedidos.

FASE 10
Identificar flujo actual de separaciones.

FASE 11
Identificar si ya existe Payment.

FASE 12
Identificar si ya existe facturación.

FASE 13
Identificar series y correlativos.

FASE 14
Diseñar integración PaymentService.

FASE 15
Implementar NiubizProvider.

FASE 16
Integrar Niubiz con POS.

FASE 17
Integrar Niubiz con Caja.

FASE 18
Integrar Niubiz con Pedidos/Separaciones cuando corresponda.

FASE 19
Diseñar BillingService.

FASE 20
Implementar VerifactProvider.

FASE 21
Integrar facturación con ventas.

FASE 22
Integrar SUNAT mediante VERIFACT.

FASE 23
Integrar comprobantes con clientes.

FASE 24
Integrar Reportes.

FASE 25
Integrar Auditoría.

FASE 26
Implementar seguridad.

FASE 27
Implementar idempotencia.

FASE 28
Implementar retries.

FASE 29
Crear pruebas unitarias.

FASE 30
Crear pruebas de integración.

FASE 31
Crear pruebas multi-tenant.

FASE 32
Crear pruebas end-to-end.

## 57. Reglas Para Claude Code

Antes de modificar archivos:

## 1. Analiza La Estructura Completa Del Proyecto.
## 2. Identifica Backend Y Frontend.
## 3. Identifica La Base De Datos.
## 4. Identifica El Sistema De Autenticación.
## 5. Identifica El Sistema Multi-Tenant.
## 6. Identifica Las Entidades Existentes.
## 7. Identifica Los Servicios Existentes.
## 8. Identifica Los Endpoints Existentes.
## 9. Identifica El Flujo Actual De Ventas.
## 10. Identifica El Flujo Actual De Caja.
## 11. Identifica El Flujo Actual De Inventario.
## 12. Identifica El Flujo Actual De Facturación.
## 13. Identifica Configuraciones Existentes.

Después:

## 14. Presenta Un Diagnóstico.
## 15. Indica Qué Ya Existe.
## 16. Indica Qué Falta.
## 17. Indica Qué Archivos/Módulos Se Modificarán.
## 18. Indica Qué Entidades Se Reutilizarán.
## 19. Indica Qué Entidades Deberán Ampliarse.
## 20. Indica Qué Nuevas Abstracciones Son Necesarias.
## 21. Presenta El Plan.
## 22. Luego Implementa.

NO modificar código innecesariamente.

NO hacer refactors masivos que no sean necesarios.

NO reemplazar módulos funcionales.

NO cambiar tecnologías existentes sin justificación.

NO crear sistemas paralelos.

## 58. Documentación Obligatoria

Al finalizar crear/actualizar documentación técnica que explique:

- arquitectura
- multi-tenancy
- Niubiz
- PaymentService
- PaymentProvider
- Verifact
- BillingService
- ElectronicInvoicingProvider
- flujo de venta
- flujo de pago
- flujo de facturación
- estados
- retries
- idempotencia
- seguridad
- configuración por tenant
- manejo de errores
- pruebas

## 59. Criterio Final De Éxito

La implementación será considerada correcta cuando:

Un negocio se registre en el SaaS.

Ese negocio configure:

- información empresarial
- RUC
- facturación
- VERIFACT
- Niubiz

y pueda utilizar el sistema normalmente.

Entonces:

PRODUCTO
 ↓
VENTA
 ↓
POS
 ↓
CLIENTE
 ↓
NIUBIZ
 ↓
PAGO APROBADO
 ↓
VENTA CONFIRMADA
 ↓
CAJA
 ↓
INVENTARIO
 ↓
VERIFACT
 ↓
SUNAT
 ↓
COMPROBANTE ACEPTADO
 ↓
CLIENTE

Mientras simultáneamente otro negocio pueda realizar exactamente
el mismo flujo utilizando:

- sus propios productos
- sus propios clientes
- sus propias ventas
- su propia caja
- su propio inventario
- su propio Merchant ID Niubiz
- sus propias credenciales
- su propio RUC
- su propia configuración VERIFACT

sin que ningún dato se mezcle.

## 60. Reglas Absolutas

NO romper funcionalidades existentes.

NO duplicar entidades existentes.

NO crear sistemas paralelos.

NO mezclar tenants.

NO utilizar credenciales de otro tenant.

NO exponer credenciales.

NO confiar en el frontend para validar pagos.

NO confiar en el frontend para determinar tenant.

NO asumir que pago aprobado significa comprobante aceptado.

NO generar comprobantes duplicados.

NO generar pagos duplicados.

NO generar movimientos de caja duplicados.

NO descontar inventario dos veces.

NO inventar APIs.

NO inventar endpoints.

NO inventar credenciales.

NO inventar parámetros.

NO utilizar documentación obsoleta.

UTILIZAR SIEMPRE LA DOCUMENTACIÓN OFICIAL VIGENTE
DE NIUBIZ, VERIFACT Y SUNAT.

RESULTADO ESPERADO

Quiero convertir el sistema existente en una plataforma SaaS
comercial profesional donde cada negocio pueda operar de forma
independiente dentro de la misma plataforma y conectar sus propias
integraciones de:

NIUBIZ
+
VERIFACT
+
SUNAT

manteniendo una arquitectura:

- Multi-tenant
- Segura
- Escalable
- Desacoplada
- Transaccional
- Auditable
- Mantenible
- Preparada para múltiples proveedores
- Preparada para cientos o miles de negocios

SIN REESCRIBIR EL SISTEMA EXISTENTE.

PRIMERO AUDITA.

DESPUÉS PROPÓN.

DESPUÉS IMPLEMENTA.

Y ANTES DE DAR POR TERMINADO EL TRABAJO,
REALIZA LAS PRUEBAS MULTI-TENANT Y END-TO-END.
