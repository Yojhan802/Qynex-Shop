-- Distingue "ver el módulo de ventas" (VENTAS_CONSULTAR, lo tienen los tres
-- roles operativos) de "ver las ventas de todos" (VENTAS_CONSULTAR_TODAS):
-- un vendedor solo debe ver sus propias ventas (docs/04-reglas-negocio.md,
-- nota "propias" en la matriz rol -> permisos).

INSERT INTO permissions (code, module, description) VALUES
    ('VENTAS_CONSULTAR_TODAS', 'VENTAS', 'Consultar las ventas de todos los vendedores, no solo las propias');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code IN ('ADMINISTRADOR', 'SUPERVISOR') AND p.code = 'VENTAS_CONSULTAR_TODAS';
