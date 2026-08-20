# 07 — Seguridad

## 1. Autenticación

### Esquema de tokens

| Token | Vida | Dónde vive | Revocable |
|---|---|---|---|
| Access token (JWT) | 30 min | Memoria del cliente | No (por eso dura poco) |
| Refresh token | 7 días | `refresh_tokens`, **hasheado** | Sí |

El access token es un JWT firmado con HMAC-SHA256; el servidor no guarda estado,
así que verificarlo no cuesta una consulta. Su corta vida limita el daño si se
filtra. El refresh token sí se persiste **hasheado con SHA-256**: si alguien
obtuviera un volcado de la base de datos, no podría suplantar ninguna sesión.

**Contenido del JWT:**
```json
{
  "sub": "3",
  "username": "carlos",
  "authorities": ["VENTAS_CREAR", "CLIENTES_CREAR"],
  "iat": 1755640331,
  "exp": 1755642131
}
```

Los permisos viajan dentro del token para evitar una consulta por petición. El
precio es que un cambio de permisos tarda hasta 30 minutos en aplicarse; cuando
el cambio debe ser inmediato (bloquear a un usuario), se revocan sus refresh
tokens y se marca el usuario como `BLOCKED`, lo que corta el acceso en el
siguiente refresco.

### Contraseñas

- **BCrypt con factor 12.** El factor 12 está calibrado para costar ~250 ms por
  verificación: imperceptible al iniciar sesión, prohibitivo para fuerza bruta.
- Nunca en texto plano, nunca en logs, nunca en respuestas de la API.
- Política mínima: 8 caracteres, al menos una letra y un número.
- El administrador puede forzar un cambio en el primer acceso
  (`must_change_password`), pero **no puede ver** la contraseña de nadie.

### Bloqueo por intentos fallidos

```
intento fallido → failed_attempts++
failed_attempts >= 5 → locked_until = ahora + 15 min
login correcto → failed_attempts = 0, locked_until = null
```

El mensaje de error es siempre el mismo (*"Usuario o contraseña incorrectos"*),
tanto si el usuario no existe como si la contraseña falla: distinguirlos permite
enumerar qué cuentas existen.

---

## 2. Autorización

### Por permiso, nunca por rol

```java
@PreAuthorize("hasAuthority('VENTAS_ANULAR')")
public void anularVenta(Long id, String motivo) { ... }
```

Un rol es solo un conjunto de permisos guardado en la base de datos. Cambiar qué
puede hacer un supervisor es modificar filas de `role_permissions`, no
recompilar. Añadir un rol nuevo ("Cajero de fin de semana") no toca una sola
línea de código.

### Filtrado por propiedad

Algunos permisos no bastan con concederse o negarse: un vendedor puede consultar
ventas, **pero solo las suyas**. Eso no se resuelve con anotaciones, sino en el
service:

```java
if (!usuarioActual.tienePermiso("VENTAS_CONSULTAR_TODAS")) {
    filtro.setUserId(usuarioActual.getId());
}
```

El filtro se impone en el servidor. Un cliente que manipule la petición para pedir
las ventas de otro no obtiene nada.

### Defensa en profundidad

| Capa | Qué protege |
|---|---|
| Frontend | Oculta lo que el usuario no puede hacer — **comodidad, no seguridad** |
| Controller | `@PreAuthorize` sobre el permiso requerido |
| Service | Reglas de negocio y filtrado por propiedad |
| Base de datos | `CHECK`, `UNIQUE`, `FOREIGN KEY` |

Ocultar un botón no protege nada: quien conozca la URL puede llamarla igual. Por
eso las cuatro capas comprueban, y la última palabra siempre la tiene el servidor.

---

## 3. Protección de la API

### CORS

Orígenes permitidos por configuración (`app.cors.allowed-origins`), nunca `*`
combinado con credenciales. En desarrollo, el origen del servidor estático del
frontend; en producción, el dominio real.

### Cabeceras de seguridad

```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Referrer-Policy: strict-origin-when-cross-origin
Content-Security-Policy: default-src 'self'
Strict-Transport-Security: max-age=31536000   (solo en producción con HTTPS)
```

### Limitación de peticiones

`POST /api/auth/login` está limitado por IP y por usuario (10 intentos por
minuto). Es el único endpoint accesible sin autenticar, y por tanto el único
expuesto a fuerza bruta desde fuera.

### CSRF

Deshabilitado **deliberadamente**: la API no usa cookies de sesión. El token va
en la cabecera `Authorization`, que un navegador no adjunta automáticamente en
una petición entre sitios, de modo que el vector CSRF no existe. Si en el futuro
se pasara a cookies, habría que reactivarlo.

### Inyección SQL

Todo el acceso a datos pasa por Spring Data JPA con consultas parametrizadas.
Las consultas nativas de reportes usan parámetros con nombre. **Nunca** se
concatena entrada del usuario en una consulta, ni siquiera en el `ORDER BY`: los
campos de ordenación se validan contra una lista blanca.

### Validación de entrada

- Bean Validation (`@NotBlank`, `@Positive`, `@Size`, `@Email`) en todos los DTO
  de entrada, activada con `@Valid` en el controller.
- Los DTO de entrada **no incluyen** campos que el cliente no debe controlar
  (`id`, `createdAt`, `status` de venta): si no está en el DTO, no se puede
  manipular.
- Tamaño máximo de subida: 5 MB, restringido a `image/png`, `image/jpeg` y
  `image/webp`, verificando el contenido real y no solo la extensión.

---

## 4. Gestión de secretos

**Nada de credenciales en el código ni en el repositorio.** Toda configuración
sensible se lee de variables de entorno:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
app:
  jwt:
    secret: ${JWT_SECRET}
```

- `.env.example` se versiona con las claves y **sin** valores.
- `.env` está en `.gitignore`.
- La aplicación **no arranca** si falta `JWT_SECRET` o si mide menos de 32
  caracteres: fallar al inicio es preferible a funcionar con una firma débil.
- Los secretos de producción se gestionan fuera del repositorio (variables del
  servidor o gestor de secretos).

---

## 5. Registro y trazabilidad

### Logging

| Nivel | Qué se registra |
|---|---|
| ERROR | Excepciones no controladas, fallos de integridad |
| WARN | Reglas de negocio violadas, accesos denegados, bloqueos de cuenta |
| INFO | Arranque, migraciones, operaciones de negocio relevantes |
| DEBUG | Solo en desarrollo |

**Nunca se registra:** contraseñas (ni hasheadas), tokens completos, números de
tarjeta, ni el cuerpo íntegro de peticiones de autenticación. Cuando hay que
referirse a un token en el log, se usan sus 8 primeros caracteres.

Cada petición lleva un identificador de correlación (`X-Request-Id`) para poder
seguir una operación completa entre logs.

### Auditoría

Distinta del logging: el log es para diagnosticar, la auditoría es para responder
"quién hizo qué y cuándo". Se guarda en `audit_logs` con usuario, acción,
entidad, valores anterior y nuevo, resultado, IP y fecha.

Se escribe con `Propagation.REQUIRES_NEW`, de modo que **sobrevive al rollback**
de la transacción principal. Un intento de anular una venta sin permiso falla, no
cambia nada… y queda registrado. Ese registro es justamente el que interesa.

---

## 6. Datos sensibles

| Dato | Tratamiento |
|---|---|
| Contraseñas | BCrypt, irreversible |
| Refresh tokens | SHA-256 en base de datos |
| DNI de clientes | En claro (necesario para operar), acceso restringido por permiso |
| Datos de Yape/Plin | En claro (son públicos: el cliente los ve para pagar), edición restringida a `CONFIGURACION_PAGOS` |
| Tarjetas | **No se almacenan**. Solo el número de operación del datáfono |

No se guarda ningún dato de tarjeta: el cobro lo hace el terminal del banco y el
sistema solo anota la referencia. Así el proyecto queda fuera del alcance de PCI-DSS.

---

## 7. Producción

- **HTTPS obligatorio**, con redirección desde HTTP y HSTS activo.
- Terminación TLS en el proxy inverso (Nginx o Caddy).
- MySQL sin exposición al exterior: solo accesible desde la red interna.
- Usuario de base de datos con los privilegios mínimos (sin `DROP`, sin `GRANT`).
- Copias de seguridad diarias automatizadas, con restauración probada.
- Actualización periódica de dependencias; `mvn dependency-check` en el proceso
  de integración.

---

## 8. Comprobaciones antes de publicar

```
□ Sin secretos en el código ni en el historial de Git
□ JWT_SECRET de al menos 32 caracteres, único por entorno
□ CORS restringido al dominio real
□ HTTPS activo con HSTS
□ ddl-auto en 'validate'
□ Contraseña del administrador inicial cambiada
□ Usuario de base de datos con privilegios mínimos
□ Logs sin datos sensibles
□ Copias de seguridad verificadas
□ Limitación de peticiones activa en /api/auth/login
□ Endpoints de diagnóstico (actuator) protegidos
```
