-- Datos semilla: permisos, roles del sistema, matriz rol->permiso y usuario
-- administrador inicial. Ver docs/04-reglas-negocio.md (matriz rol -> permisos).

INSERT INTO permissions (code, module, description) VALUES
    ('DASHBOARD_VER',          'DASHBOARD',     'Ver el panel principal'),

    ('PRODUCTOS_CONSULTAR',    'PRODUCTOS',     'Consultar productos'),
    ('PRODUCTOS_CREAR',        'PRODUCTOS',     'Crear productos'),
    ('PRODUCTOS_EDITAR',       'PRODUCTOS',     'Editar productos'),
    ('PRODUCTOS_ELIMINAR',     'PRODUCTOS',     'Eliminar productos'),
    ('VARIANTES_GESTIONAR',    'PRODUCTOS',     'Crear y editar variantes'),
    ('BARCODE_GENERAR',        'PRODUCTOS',     'Generar y asignar códigos de barras'),

    ('INVENTARIO_CONSULTAR',   'INVENTARIO',    'Consultar inventario'),
    ('INVENTARIO_ENTRADA',     'INVENTARIO',    'Registrar entradas de inventario'),
    ('INVENTARIO_SALIDA',      'INVENTARIO',    'Registrar salidas de inventario'),
    ('INVENTARIO_AJUSTAR',     'INVENTARIO',    'Registrar ajustes de inventario'),

    ('VENTAS_CONSULTAR',       'VENTAS',        'Consultar ventas'),
    ('VENTAS_CREAR',           'VENTAS',        'Registrar ventas en el POS'),
    ('VENTAS_ANULAR',          'VENTAS',        'Anular ventas'),
    ('VENTAS_DESCUENTO',       'VENTAS',        'Aplicar descuentos en el POS'),
    ('VENTAS_DEVOLVER',        'VENTAS',        'Registrar devoluciones'),

    ('CLIENTES_CONSULTAR',     'CLIENTES',      'Consultar clientes'),
    ('CLIENTES_CREAR',         'CLIENTES',      'Crear clientes'),
    ('CLIENTES_EDITAR',        'CLIENTES',      'Editar clientes'),

    ('CAJA_ABRIR',             'CAJA',          'Abrir sesión de caja'),
    ('CAJA_CERRAR',            'CAJA',          'Cerrar sesión de caja'),
    ('CAJA_CONSULTAR',         'CAJA',          'Consultar cajas y sesiones'),
    ('CAJA_MOVIMIENTO',        'CAJA',          'Registrar ingresos, gastos y retiros'),

    ('REPORTES_CONSULTAR',     'REPORTES',      'Consultar reportes'),
    ('REPORTES_EXPORTAR',      'REPORTES',      'Exportar reportes'),

    ('AUDITORIA_CONSULTAR',    'AUDITORIA',     'Consultar el registro de auditoría'),

    ('USUARIOS_CONSULTAR',     'USUARIOS',      'Consultar usuarios'),
    ('USUARIOS_CREAR',         'USUARIOS',      'Crear usuarios'),
    ('USUARIOS_EDITAR',        'USUARIOS',      'Editar usuarios'),
    ('USUARIOS_BLOQUEAR',      'USUARIOS',      'Activar, desactivar o bloquear usuarios'),
    ('ROLES_GESTIONAR',        'USUARIOS',      'Gestionar roles y sus permisos'),

    ('CONFIGURACION_VER',      'CONFIGURACION', 'Ver la configuración del sistema'),
    ('CONFIGURACION_EDITAR',   'CONFIGURACION', 'Editar la configuración del sistema'),
    ('CONFIGURACION_PAGOS',    'CONFIGURACION', 'Editar los datos oficiales de pago (Yape/Plin)');

INSERT INTO roles (code, name, description, is_system) VALUES
    ('ADMINISTRADOR', 'Administrador', 'Acceso completo al sistema', TRUE),
    ('SUPERVISOR',    'Supervisor',    'Ventas, caja, inventario, reportes y clientes', TRUE),
    ('VENDEDOR',      'Vendedor',      'Ventas, consulta de productos y clientes', TRUE),
    ('ALMACENERO',    'Almacenero',    'Consulta de productos y gestión de inventario', TRUE);

-- ADMINISTRADOR: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMINISTRADOR';

-- SUPERVISOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPERVISOR' AND p.code IN (
    'DASHBOARD_VER',
    'PRODUCTOS_CONSULTAR', 'PRODUCTOS_CREAR', 'PRODUCTOS_EDITAR',
    'VARIANTES_GESTIONAR', 'BARCODE_GENERAR',
    'INVENTARIO_CONSULTAR', 'INVENTARIO_ENTRADA', 'INVENTARIO_SALIDA', 'INVENTARIO_AJUSTAR',
    'VENTAS_CONSULTAR', 'VENTAS_CREAR', 'VENTAS_ANULAR', 'VENTAS_DESCUENTO', 'VENTAS_DEVOLVER',
    'CLIENTES_CONSULTAR', 'CLIENTES_CREAR', 'CLIENTES_EDITAR',
    'CAJA_ABRIR', 'CAJA_CERRAR', 'CAJA_CONSULTAR', 'CAJA_MOVIMIENTO',
    'REPORTES_CONSULTAR', 'REPORTES_EXPORTAR',
    'CONFIGURACION_VER'
);

-- VENDEDOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'VENDEDOR' AND p.code IN (
    'DASHBOARD_VER',
    'PRODUCTOS_CONSULTAR',
    'INVENTARIO_CONSULTAR',
    'VENTAS_CONSULTAR', 'VENTAS_CREAR',
    'CLIENTES_CONSULTAR', 'CLIENTES_CREAR',
    'CAJA_ABRIR', 'CAJA_CONSULTAR'
);

-- ALMACENERO
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ALMACENERO' AND p.code IN (
    'DASHBOARD_VER',
    'PRODUCTOS_CONSULTAR', 'VARIANTES_GESTIONAR', 'BARCODE_GENERAR',
    'INVENTARIO_CONSULTAR', 'INVENTARIO_ENTRADA', 'INVENTARIO_SALIDA', 'INVENTARIO_AJUSTAR'
);

-- Usuario administrador inicial.
-- Usuario: admin  ·  Contraseña temporal: FreestylePeru#2026  (debe cambiarse en el primer acceso)
INSERT INTO users (username, email, password_hash, full_name, status, must_change_password, created_at, updated_at)
VALUES (
    'admin',
    'admin@freestyleperu.pe',
    '$2a$12$8il/EMVkfUaS91rq8PmUKesmmYASm1WS9UrAlwdOqSpYKP1Gnoud6',
    'Administrador del Sistema',
    'ACTIVE',
    TRUE,
    UTC_TIMESTAMP(6),
    UTC_TIMESTAMP(6)
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r WHERE u.username = 'admin' AND r.code = 'ADMINISTRADOR';
