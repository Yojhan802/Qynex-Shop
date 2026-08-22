-- Techo de asignación de roles: un usuario solo puede asignar, al crear a
-- otro usuario, roles cuyo hierarchy_level sea <= el nivel más alto entre
-- sus propios roles. Así un "Jefe de Tienda" puede crear cajeros/vendedores
-- (y hasta otro Jefe de Tienda a su mismo nivel) sin poder crear un
-- Administrador. Ver docs/04-reglas-negocio.md RN-25.

ALTER TABLE roles ADD COLUMN hierarchy_level SMALLINT NOT NULL DEFAULT 0;

UPDATE roles SET hierarchy_level = 100 WHERE code = 'ADMINISTRADOR';
UPDATE roles SET hierarchy_level = 50  WHERE code = 'SUPERVISOR';
UPDATE roles SET hierarchy_level = 10  WHERE code = 'VENDEDOR';
UPDATE roles SET hierarchy_level = 10  WHERE code = 'ALMACENERO';
