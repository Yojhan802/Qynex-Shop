-- Ahora que existe un techo de asignación por rol (hierarchy_level, RN-25),
-- darle a SUPERVISOR permiso para gestionar usuarios es seguro: su techo
-- (50) le permite crear/editar personal de nivel igual o menor (otro
-- Supervisor, Vendedor, Almacenero o un rol de cliente como "Jefe de
-- Tienda"), pero nunca un Administrador (nivel 100). Antes de RN-25 esto
-- habría sido peligroso — es la pieza que faltaba para delegar la gestión
-- de personal sin delegar el control total del sistema.

-- INSERT IGNORE porque esta instalación ya tenía el permiso concedido a mano
-- desde el panel de Roles antes de que esta migración existiera.
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPERVISOR'
  AND p.code IN ('USUARIOS_CONSULTAR', 'USUARIOS_CREAR', 'USUARIOS_EDITAR', 'USUARIOS_BLOQUEAR');
